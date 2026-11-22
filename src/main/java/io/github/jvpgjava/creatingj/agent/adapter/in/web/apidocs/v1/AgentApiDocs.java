package io.github.jvpgjava.creatingj.agent.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunDetailResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunSummaryResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunsResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.CreateAgentRunRequest;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.ToolProposalDecisionRequest;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.ToolProposalDecisionResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Agent Orchestration", description = "Planejamento de agente, contexto de workspace e guardrails de tools")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface AgentApiDocs {

    @Operation(
            operationId = "createAgentRun",
            summary = "Planejar acoes de agente com contexto do workspace, RAG e guardrails deterministicos",
            description = "Recebe objetivo, snapshots opcionais do workspace e consulta RAG do projeto. O servidor "
                    + "planeja propostas de ferramentas e aplica a politica deterministica sem executar comandos "
                    + "locais."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano e propostas avaliadas criados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AgentRunDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Entrada ou snapshot de workspace invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Quota ou limite de concorrencia excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<AgentRunDetailResponse> createRun(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            CreateAgentRunRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "listAgentRuns",
            summary = "Listar execucoes recentes de agente do projeto",
            description = "Retorna as execucoes do projeto pertencente ao usuario autenticado em ordem decrescente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de execucoes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AgentRunsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<AgentRunsResponse> listRuns(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            @Parameter(description = "Quantidade maxima de execucoes retornadas (1..50)", example = "20")
            int limit,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "getAgentRun",
            summary = "Consultar detalhes e propostas de uma execucao do agente",
            description = "Retorna o resumo do plano e todas as propostas de ferramenta com seus vereditos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes da execucao",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AgentRunDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Execucao ou projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<AgentRunDetailResponse> getRun(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            @Parameter(description = "Identificador UUID da execucao do agente",
                    example = "550e8400-e29b-41d4-a716-446655440040")
            String runId,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "decideToolProposal",
            summary = "Aprovar ou rejeitar proposta pendente de ferramenta do agente",
            description = "Registra a decisao humana sobre uma proposta classificada como REQUIRE_APPROVAL. "
                    + "Propostas bloqueadas por politica (DENY) nunca podem ser aprovadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decisao registrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ToolProposalDecisionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado invalido ou proposta negada por politica",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Execucao ou proposta nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ToolProposalDecisionResponse> decideProposal(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            @Parameter(description = "Identificador UUID da execucao do agente",
                    example = "550e8400-e29b-41d4-a716-446655440040")
            String runId,
            @Parameter(description = "Identificador UUID da proposta de ferramenta",
                    example = "550e8400-e29b-41d4-a716-446655440041")
            String proposalId,
            ToolProposalDecisionRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "cancelAgentRun",
            summary = "Cancelar execucao do agente e rejeitar propostas pendentes",
            description = "Encerra a execucao imediatamente e marca todas as propostas pendentes como rejeitadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execucao cancelada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AgentRunSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Execucao nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<AgentRunSummaryResponse> cancelRun(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            @Parameter(description = "Identificador UUID da execucao do agente",
                    example = "550e8400-e29b-41d4-a716-446655440040")
            String runId,
            AuthenticatedUser actor
    );
}
