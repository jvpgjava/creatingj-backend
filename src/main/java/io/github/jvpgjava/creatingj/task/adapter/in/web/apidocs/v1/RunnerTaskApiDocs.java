package io.github.jvpgjava.creatingj.task.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.security.AuthenticatedDevice;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskSummaryResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerApprovalRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerCompleteTaskRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerLeaseResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerTaskEventRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerTaskEventResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalResponse;
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

@Tag(name = "Runner Protocol", description = "Endpoints exclusivos do runner local (creatingj-agent)")
@SecurityRequirement(name = "deviceTokenAuth")
public interface RunnerTaskApiDocs {

    @Operation(
            operationId = "leaseNextRunnerTask",
            summary = "Adquirir lease da proxima tarefa destinada ao runner",
            description = "Consulta a fila de tarefas do dispositivo autenticado via X-Device-Token e adquire "
                    + "lease temporario da proxima tarefa pendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado do lease",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RunnerLeaseResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credencial de dispositivo invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RunnerLeaseResponse> leaseNext(AuthenticatedDevice device);

    @Operation(
            operationId = "reportRunnerTaskEvent",
            summary = "Reportar evento de progresso com deduplicacao",
            description = "Persiste evento de progresso da tarefa e propaga em tempo real via SSE, deduplicando "
                    + "retentativas pela chave eventKey."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento registrado ou deduplicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RunnerTaskEventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada para este dispositivo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RunnerTaskEventResponse> reportEvent(
            @Parameter(description = "Identificador UUID da tarefa atribuida ao dispositivo",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            RunnerTaskEventRequest request,
            AuthenticatedDevice device
    );

    @Operation(
            operationId = "requestRunnerToolApproval",
            summary = "Solicitar aprovacao pontual com TTL para tool",
            description = "Submete ferramenta/comando para avaliacao pela politica deterministica e cria pedido "
                    + "de aprovacao humana de uso unico com expiracao configurada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aprovacao criada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ToolApprovalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ferramenta ou comando bloqueado por politica",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada para este dispositivo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ToolApprovalResponse> requestApproval(
            @Parameter(description = "Identificador UUID da tarefa atribuida ao dispositivo",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            RunnerApprovalRequest request,
            AuthenticatedDevice device
    );

    @Operation(
            operationId = "completeRunnerTask",
            summary = "Concluir ou falhar tarefa atribuida ao dispositivo",
            description = "Marca a tarefa como COMPLETED ou FAILED e registra o resumo final de execucao."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarefa finalizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RemoteTaskSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada para este dispositivo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<RemoteTaskSummaryResponse> completeTask(
            @Parameter(description = "Identificador UUID da tarefa atribuida ao dispositivo",
                    example = "550e8400-e29b-41d4-a716-446655440060")
            String taskId,
            RunnerCompleteTaskRequest request,
            AuthenticatedDevice device
    );
}
