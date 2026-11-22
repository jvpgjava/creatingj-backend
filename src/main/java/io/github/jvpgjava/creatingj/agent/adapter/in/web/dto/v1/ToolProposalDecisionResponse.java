package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado atualizado da execucao e da proposta apos decisao de aprovacao ou rejeicao")
public record ToolProposalDecisionResponse(
        @Schema(description = "Resumo atualizado da execucao do agente")
        AgentRunSummaryResponse run,
        @Schema(description = "Proposta com status de aprovacao atualizado")
        AgentToolProposalResponse proposal
) {
}
