package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do pedido de lease de proxima tarefa pendente para o dispositivo")
public record RunnerLeaseResponse(
        @Schema(description = "Indica se havia tarefa disponivel e atribuida ao runner", example = "true")
        boolean assigned,
        @Schema(description = "Dados da tarefa atribuida (nulo quando assigned for false)", nullable = true)
        RemoteTaskSummaryResponse task
) {
}
