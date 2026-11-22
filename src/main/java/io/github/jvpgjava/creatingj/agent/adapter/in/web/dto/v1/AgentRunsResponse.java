package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lista de execucoes de agente vinculadas ao projeto")
public record AgentRunsResponse(
        @Schema(description = "Execucoes ordenadas da mais recente para a mais antiga")
        List<AgentRunSummaryResponse> items
) {
}
