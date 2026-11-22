package io.github.jvpgjava.creatingj.agent.domain;

import java.util.List;

public record ParsedAgentPlan(
        String planSummary,
        List<ProposedToolAction> proposals
) {
    public ParsedAgentPlan {
        planSummary = (planSummary == null || planSummary.isBlank())
                ? "Plano gerado sem acoes adicionais."
                : planSummary.trim();
        proposals = proposals == null ? List.of() : List.copyOf(proposals);
    }
}
