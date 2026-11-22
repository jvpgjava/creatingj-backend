package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import java.util.List;

public record AgentRunDetailResponse(
        AgentRunSummaryResponse run,
        List<AgentToolProposalResponse> proposals
) {
}
