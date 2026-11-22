package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

public record KnowledgeSearchMatchResponse(
        String sourceId,
        String sourcePath,
        String title,
        int chunkIndex,
        String excerpt,
        double score,
        String embeddingVersion
) {
}
