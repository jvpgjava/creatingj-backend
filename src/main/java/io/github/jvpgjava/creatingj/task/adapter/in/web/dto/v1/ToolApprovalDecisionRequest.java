package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import jakarta.validation.constraints.NotNull;

public record ToolApprovalDecisionRequest(
        @NotNull(message = "Acao de decisao obrigatoria")
        ToolDecisionAction action
) {
}
