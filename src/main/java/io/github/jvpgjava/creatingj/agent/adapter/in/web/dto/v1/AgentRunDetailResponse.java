package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detalhes completos de uma execucao do agente e suas propostas de ferramentas")
public record AgentRunDetailResponse(
        @Schema(description = "Resumo da execucao do agente")
        AgentRunSummaryResponse run,
        @Schema(description = "Propostas de acoes/ferramentas avaliadas pela politica deterministica de seguranca")
        List<AgentToolProposalResponse> proposals
) {
}
