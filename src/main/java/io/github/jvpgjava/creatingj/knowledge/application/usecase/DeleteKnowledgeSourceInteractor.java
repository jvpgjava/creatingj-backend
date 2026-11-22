package io.github.jvpgjava.creatingj.knowledge.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.DeleteKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeIndexJobStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeMetricsPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeJobOperation;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.time.Instant;
import java.util.UUID;

public final class DeleteKnowledgeSourceInteractor implements DeleteKnowledgeSourceUseCase {

    private final ProjectStorePort projectStore;
    private final KnowledgeSourceStorePort sourceStore;
    private final VectorKnowledgePort vectorStore;
    private final KnowledgeIndexJobStorePort jobStore;
    private final KnowledgeMetricsPort metricsPort;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public DeleteKnowledgeSourceInteractor(
            ProjectStorePort projectStore,
            KnowledgeSourceStorePort sourceStore,
            VectorKnowledgePort vectorStore,
            KnowledgeIndexJobStorePort jobStore,
            KnowledgeMetricsPort metricsPort,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.projectStore = projectStore;
        this.sourceStore = sourceStore;
        this.vectorStore = vectorStore;
        this.jobStore = jobStore;
        this.metricsPort = metricsPort;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void execute(Command command) {
        if (command == null || command.sourceId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da fonte obrigatorio.");
        }
        ProjectAuthorizationGuard.requireOwnedProject(projectStore, command.actorUserId(), command.projectId());
        KnowledgeSource source = sourceStore.findById(command.sourceId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Fonte de conhecimento nao encontrada."));
        if (!source.belongsTo(command.projectId(), command.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Fonte de conhecimento nao encontrada.");
        }
        Instant now = clock.now();
        vectorStore.deleteBySource(source.id());
        sourceStore.deleteById(source.id());
        jobStore.save(KnowledgeIndexJob.pending(
                command.projectId(),
                command.actorUserId(),
                null,
                source.sourcePath(),
                KnowledgeJobOperation.DELETE,
                now
        ).complete(null, 0, clock.now()));
        metricsPort.recordSourceDeletion();
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.KNOWLEDGE_SOURCE_DELETED,
                "SUCCESS",
                "projectId=" + command.projectId() + " sourceId=" + source.id(),
                clock.now()
        ));
    }
}
