package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import java.time.Instant;

public record AgentToolProposalResponse(
        String id,
        int stepIndex,
        String toolName,
        String targetPath,
        String commandText,
        String rationale,
        String policyDecision,
        String policyReason,
        String approvalStatus,
        Instant decidedAt,
        Instant createdAt
) {
}
