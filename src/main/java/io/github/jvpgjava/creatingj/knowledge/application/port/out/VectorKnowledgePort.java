package io.github.jvpgjava.creatingj.knowledge.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunk;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface VectorKnowledgePort {

    void replaceSourceChunks(KnowledgeSourceId sourceId, List<KnowledgeChunk> chunks);

    List<KnowledgeSearchMatch> searchSimilar(
            ProjectId projectId,
            UserId ownerUserId,
            EmbeddingVector queryVector,
            String embeddingVersion,
            int topK,
            double minScore
    );

    default List<KnowledgeSearchMatch> searchHybrid(
            ProjectId projectId,
            UserId ownerUserId,
            String queryText,
            EmbeddingVector queryVector,
            String embeddingVersion,
            int topK,
            double minScore
    ) {
        return searchSimilar(projectId, ownerUserId, queryVector, embeddingVersion, topK, minScore);
    }

    void deleteBySource(KnowledgeSourceId sourceId);

    void deleteByProject(ProjectId projectId, UserId ownerUserId);
}
