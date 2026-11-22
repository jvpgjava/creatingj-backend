package io.github.jvpgjava.creatingj.knowledge.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface SearchProjectKnowledgeUseCase {

    Result execute(Query query);

    record Query(UserId actorUserId, ProjectId projectId, String queryText, int topK, double minScore) {
    }

    record Result(
            ProjectId projectId,
            String embeddingVersion,
            long latencyMs,
            List<KnowledgeSearchMatch> matches
    ) {
    }
}
