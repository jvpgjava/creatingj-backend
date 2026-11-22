package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import java.util.List;

public record KnowledgeSearchResponse(
        String projectId,
        String embeddingVersion,
        long latencyMs,
        List<KnowledgeSearchMatchResponse> matches
) {
}
