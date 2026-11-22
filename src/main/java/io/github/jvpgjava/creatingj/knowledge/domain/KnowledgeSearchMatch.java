package io.github.jvpgjava.creatingj.knowledge.domain;

public record KnowledgeSearchMatch(
        KnowledgeSourceId sourceId,
        String sourcePath,
        String title,
        int chunkIndex,
        String excerpt,
        double score,
        String embeddingVersion
) {
}
