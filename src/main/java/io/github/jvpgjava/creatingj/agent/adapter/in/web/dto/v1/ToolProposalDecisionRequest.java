package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import jakarta.validation.constraints.NotNull;

public record ToolProposalDecisionRequest(
        @NotNull(message = "Acao de decisao obrigatoria")
        ToolDecisionAction action
) {
}
