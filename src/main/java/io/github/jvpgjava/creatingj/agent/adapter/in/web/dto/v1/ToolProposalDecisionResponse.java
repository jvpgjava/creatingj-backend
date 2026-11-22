package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

public record ToolProposalDecisionResponse(
        AgentRunSummaryResponse run,
        AgentToolProposalResponse proposal
) {
}
