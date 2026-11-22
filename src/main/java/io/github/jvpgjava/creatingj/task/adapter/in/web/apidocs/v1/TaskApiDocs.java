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

    @Operation(operationId = "createRemoteTask", summary = "Enfileirar tarefa remota para dispositivo autorizado")
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

    @Operation(operationId = "listRemoteTasks", summary = "Listar tarefas remotas recentes do usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tarefas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RemoteTasksResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RemoteTasksResponse> list(int limit, AuthenticatedUser actor);

    @Operation(operationId = "getRemoteTask", summary = "Obter detalhes, eventos e aprovacoes de uma tarefa")
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
    ResponseEntity<RemoteTaskDetailResponse> getDetail(String taskId, AuthenticatedUser actor);

    @Operation(operationId = "cancelRemoteTask", summary = "Cancelar tarefa remota e rejeitar aprovacoes pendentes")
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
    ResponseEntity<RemoteTaskSummaryResponse> cancel(String taskId, AuthenticatedUser actor);

    @Operation(operationId = "decideRemoteToolApproval", summary = "Aprovar ou rejeitar solicitacao pontual do runner")
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
            String taskId,
            String approvalId,
            ToolApprovalDecisionRequest request,
            AuthenticatedUser actor
    );

    @Operation(operationId = "streamRemoteTaskEvents", summary = "Acompanhar progresso de uma tarefa remota via SSE")
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
    SseEmitter streamEvents(String taskId, AuthenticatedUser actor);
}
