package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import java.time.Instant;

public record AgentRunSummaryResponse(
        String id,
        String projectId,
        String goal,
        String modelId,
        String planSummary,
        String status,
        int workspaceFileCount,
        int ragMatchCount,
        Instant createdAt,
        Instant updatedAt
) {
}
