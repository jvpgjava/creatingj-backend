package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import java.time.Instant;

public record ToolApprovalResponse(
        String id,
        String taskId,
        String toolName,
        String targetPath,
        String commandText,
        String diffPreview,
        String status,
        Instant expiresAt,
        Instant decidedAt,
        Instant createdAt
) {
}
