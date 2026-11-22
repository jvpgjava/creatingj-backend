package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Decisao humana sobre uma proposta de ferramenta pendente de aprovacao")
public record ToolProposalDecisionRequest(
        @Schema(description = "Acao aplicada a proposta (APPROVE ou REJECT)", example = "APPROVE")
        @NotNull(message = "Acao de decisao obrigatoria")
        ToolDecisionAction action
) {
}
