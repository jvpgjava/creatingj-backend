package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import java.time.Instant;

public record RemoteTaskSummaryResponse(
        String id,
        String projectId,
        String deviceId,
        String title,
        String instruction,
        String status,
        Instant leaseExpiresAt,
        String resultSummary,
        String errorCode,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
