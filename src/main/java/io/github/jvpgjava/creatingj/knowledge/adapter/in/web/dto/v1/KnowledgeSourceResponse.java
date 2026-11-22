package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import java.time.Instant;

public record KnowledgeSourceResponse(
        String id,
        String projectId,
        String sourcePath,
        String title,
        String contentHash,
        String embeddingVersion,
        String status,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {
}
