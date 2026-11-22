package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import java.time.Instant;

public record KnowledgeIndexJobResponse(
        String id,
        String projectId,
        String sourceId,
        String sourcePath,
        String operation,
        String status,
        int chunksIndexed,
        String errorCode,
        Instant createdAt,
        Instant completedAt
) {
}
