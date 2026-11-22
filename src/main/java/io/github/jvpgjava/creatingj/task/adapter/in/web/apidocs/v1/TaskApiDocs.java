package io.github.jvpgjava.creatingj.task.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.CreateRemoteTaskRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.CreateRemoteTaskResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskDetailResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskSummaryResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTasksResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalDecisionRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalDecisionResponse;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Remote Tasks", description = "Fila de tarefas remotas, cancelamento, aprovacoes pontuais e SSE")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface TaskApiDocs {

    @Operation(
            operationId = "createRemoteTask",
            summary = "Enfileirar tarefa remota para dispositivo autorizado",
            description = "Enfileira tarefa para execucao pelo runner do dispositivo. Caso o dispositivo esteja "
                    + "offline, a tarefa permanece em QUEUED ate o proximo lease sem simular execucao."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tarefa criada ou deduplicada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateRemoteTaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Entrada invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Dispositivo ou projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Dispositivo revogado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<CreateRemoteTaskResponse> create(CreateRemoteTaskRequest request, AuthenticatedUser actor);

    @Operation(
            operationId = "listRemoteTasks",
            summary = "Listar tarefas remotas recentes do usuario",
            description = "Retorna as tarefas mais recentes do usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tarefas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RemoteTasksResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RemoteTasksResponse> list(
            @Parameter(description = "Quantidade maxima de tarefas retornadas (1..50)", example = "20")
            int limit,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "getRemoteTask",
            summary = "Obter detalhes, eventos e aprovacoes de uma tarefa",
            description = "Consulta o estado completo da tarefa, eventos cronologicos e aprovacoes de tools."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes da tarefa",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RemoteTaskDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RemoteTaskDetailResponse> getDetail(
            @Parameter(description = "Identificador UUID da tarefa remota",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "cancelRemoteTask",
            summary = "Cancelar tarefa remota e rejeitar aprovacoes pendentes",
            description = "Interrompe a tarefa e marca automaticamente quaisquer aprovacoes pendentes como rejeitadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa cancelada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RemoteTaskSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RemoteTaskSummaryResponse> cancel(
            @Parameter(description = "Identificador UUID da tarefa remota",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "decideRemoteToolApproval",
            summary = "Aprovar ou rejeitar solicitacao pontual do runner",
            description = "Decide um pedido de aprovacao pendente dentro do seu TTL. Cada aprovacao e de uso unico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decisao registrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ToolApprovalDecisionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa ou aprovacao nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Aprovacao expirada ou ja decidida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ToolApprovalDecisionResponse> decideApproval(
            @Parameter(description = "Identificador UUID da tarefa remota",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            @Parameter(description = "Identificador UUID do pedido de aprovacao",
                    example = "550e8400-e29b-41d4-a716-446655440062")
            String approvalId,
            ToolApprovalDecisionRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "streamRemoteTaskEvents",
            summary = "Acompanhar progresso de uma tarefa remota via SSE",
            description = "Abre fluxo text/event-stream com o snapshot inicial da tarefa e eventos subsequentes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream SSE de eventos da tarefa",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    SseEmitter streamEvents(
            @Parameter(description = "Identificador UUID da tarefa remota",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            AuthenticatedUser actor
    );
}
