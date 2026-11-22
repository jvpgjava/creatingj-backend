package io.github.jvpgjava.creatingj.knowledge.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.SearchProjectKnowledgeUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.EmbeddingPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeMetricsPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.util.List;

public final class SearchProjectKnowledgeInteractor implements SearchProjectKnowledgeUseCase {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;
    private static final int MAX_QUERY_LENGTH = 2000;

    private final ProjectStorePort projectStore;
    private final EmbeddingPort embeddingPort;
    private final VectorKnowledgePort vectorStore;
    private final KnowledgeMetricsPort metricsPort;

    public SearchProjectKnowledgeInteractor(
            ProjectStorePort projectStore,
            EmbeddingPort embeddingPort,
            VectorKnowledgePort vectorStore,
            KnowledgeMetricsPort metricsPort
    ) {
        this.projectStore = projectStore;
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
        this.metricsPort = metricsPort;
    }

    @Override
    public Result execute(Query query) {
        if (query == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Consulta RAG obrigatoria.");
        }
        ProjectAuthorizationGuard.requireOwnedProject(projectStore, query.actorUserId(), query.projectId());
        if (query.queryText() == null || query.queryText().isBlank()
                || query.queryText().trim().length() > MAX_QUERY_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Texto de busca RAG invalido.");
        }
        String normalizedQuery = query.queryText().trim();
        int topK = query.topK() <= 0 ? DEFAULT_TOP_K : Math.min(query.topK(), MAX_TOP_K);
        double minScore = Math.max(-1.0, Math.min(1.0, query.minScore()));

        long startedNanos = System.nanoTime();
        String embeddingVersion = embeddingPort.embeddingVersion();
        EmbeddingVector queryVector = embeddingPort.embedQuery(normalizedQuery);
        List<KnowledgeSearchMatch> matches = vectorStore.searchHybrid(
                query.projectId(),
                query.actorUserId(),
                normalizedQuery,
                queryVector,
                embeddingVersion,
                topK,
                minScore
        );
        long latencyMs = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
        metricsPort.recordSearch(matches.size(), latencyMs);
        return new Result(query.projectId(), embeddingVersion, latencyMs, matches);
    }
}
