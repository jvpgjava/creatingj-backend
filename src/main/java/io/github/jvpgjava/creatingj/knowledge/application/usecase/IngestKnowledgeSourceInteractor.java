package io.github.jvpgjava.creatingj.knowledge.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.inference.domain.TokenEstimator;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.IngestKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.EmbeddingPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeIndexJobStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeMetricsPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.domain.ContentHasher;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunk;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunkId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIngestionPolicy;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeJobOperation;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceStatus;
import io.github.jvpgjava.creatingj.knowledge.domain.TextChunker;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IngestKnowledgeSourceInteractor implements IngestKnowledgeSourceUseCase {

    private final ProjectStorePort projectStore;
    private final KnowledgeSourceStorePort sourceStore;
    private final VectorKnowledgePort vectorStore;
    private final KnowledgeIndexJobStorePort jobStore;
    private final EmbeddingPort embeddingPort;
    private final KnowledgeMetricsPort metricsPort;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public IngestKnowledgeSourceInteractor(
            ProjectStorePort projectStore,
            KnowledgeSourceStorePort sourceStore,
            VectorKnowledgePort vectorStore,
            KnowledgeIndexJobStorePort jobStore,
            EmbeddingPort embeddingPort,
            KnowledgeMetricsPort metricsPort,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.projectStore = projectStore;
        this.sourceStore = sourceStore;
        this.vectorStore = vectorStore;
        this.jobStore = jobStore;
        this.embeddingPort = embeddingPort;
        this.metricsPort = metricsPort;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        if (command == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Comando de ingestao obrigatorio.");
        }
        ProjectAuthorizationGuard.requireOwnedProject(projectStore, command.actorUserId(), command.projectId());
        long startedNanos = System.nanoTime();
        Instant now = clock.now();
        String normalizedPath;
        String normalizedContent;
        try {
            normalizedPath = KnowledgeIngestionPolicy.normalizeAndValidatePath(command.sourcePath());
            normalizedContent = KnowledgeIngestionPolicy.validateAndNormalizeContent(command.content());
        } catch (DomainException ex) {
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    command.actorUserId().value(),
                    command.actorUserId().value(),
                    AuditAction.KNOWLEDGE_INGESTION_REJECTED,
                    "REJECTED",
                    "projectId=" + command.projectId() + " code=" + ex.errorCode(),
                    now
            ));
            metricsPort.recordIngestion("REJECTED", 0, elapsedMs(startedNanos));
            throw ex;
        }

        String resolvedTitle = (command.title() == null || command.title().isBlank())
                ? normalizedPath
                : command.title().trim();
        String contentHash = ContentHasher.sha256Hex(normalizedContent);
        String embeddingVersion = embeddingPort.embeddingVersion();

        Optional<KnowledgeSource> existingOpt = sourceStore.findByProjectAndPath(command.projectId(), normalizedPath);
        if (existingOpt.isPresent() && existingOpt.get().isUnchanged(contentHash, embeddingVersion)) {
            KnowledgeSource existing = existingOpt.get();
            KnowledgeIndexJob job = jobStore.save(KnowledgeIndexJob.pending(
                    command.projectId(),
                    command.actorUserId(),
                    existing.id(),
                    normalizedPath,
                    KnowledgeJobOperation.UPSERT,
                    now
            ).skippedUnchanged(existing.id(), clock.now()));
            metricsPort.recordIngestion("SKIPPED_UNCHANGED", 0, elapsedMs(startedNanos));
            return new Result(existing, job, true);
        }

        KnowledgeSourceId sourceId = existingOpt.map(KnowledgeSource::id).orElseGet(KnowledgeSourceId::random);
        Instant createdAt = existingOpt.map(KnowledgeSource::createdAt).orElse(now);
        KnowledgeIndexJob runningJob = jobStore.save(KnowledgeIndexJob.pending(
                command.projectId(),
                command.actorUserId(),
                existingOpt.map(KnowledgeSource::id).orElse(null),
                normalizedPath,
                KnowledgeJobOperation.UPSERT,
                now
        ));

        try {
            List<String> chunkTexts = TextChunker.split(normalizedContent);
            KnowledgeSource pendingSource = new KnowledgeSource(
                    sourceId,
                    command.projectId(),
                    command.actorUserId(),
                    normalizedPath,
                    resolvedTitle,
                    contentHash,
                    embeddingVersion,
                    KnowledgeSourceStatus.PENDING,
                    0,
                    createdAt,
                    now
            );
            sourceStore.save(pendingSource);

            List<KnowledgeChunk> chunks = new ArrayList<>(chunkTexts.size());
            for (int i = 0; i < chunkTexts.size(); i++) {
                String chunkText = chunkTexts.get(i);
                EmbeddingVector vector = embeddingPort.embed(chunkText);
                chunks.add(new KnowledgeChunk(
                        KnowledgeChunkId.random(),
                        sourceId,
                        command.projectId(),
                        command.actorUserId(),
                        i,
                        chunkText,
                        ContentHasher.sha256Hex(chunkText),
                        TokenEstimator.estimateTokens(chunkText),
                        embeddingVersion,
                        vector,
                        clock.now()
                ));
            }
            vectorStore.replaceSourceChunks(sourceId, chunks);

            Instant finishedAt = clock.now();
            KnowledgeSource indexedSource = new KnowledgeSource(
                    sourceId,
                    command.projectId(),
                    command.actorUserId(),
                    normalizedPath,
                    resolvedTitle,
                    contentHash,
                    embeddingVersion,
                    KnowledgeSourceStatus.INDEXED,
                    chunks.size(),
                    createdAt,
                    finishedAt
            );
            sourceStore.save(indexedSource);
            KnowledgeIndexJob completedJob = jobStore.save(runningJob.complete(sourceId, chunks.size(), finishedAt));
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    command.actorUserId().value(),
                    command.actorUserId().value(),
                    AuditAction.KNOWLEDGE_SOURCE_INGESTED,
                    "SUCCESS",
                    "projectId=" + command.projectId() + " sourceId=" + sourceId + " chunks=" + chunks.size(),
                    finishedAt
            ));
            metricsPort.recordIngestion("COMPLETED", chunks.size(), elapsedMs(startedNanos));
            return new Result(indexedSource, completedJob, false);
        } catch (DomainException ex) {
            jobStore.save(runningJob.fail(ex.errorCode().name(), clock.now()));
            metricsPort.recordIngestion("FAILED", 0, elapsedMs(startedNanos));
            throw ex;
        }
    }

    private static long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }
}
