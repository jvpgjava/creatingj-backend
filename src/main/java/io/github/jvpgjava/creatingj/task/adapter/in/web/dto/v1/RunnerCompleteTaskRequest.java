package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload enviado pelo runner para finalizar uma tarefa com sucesso ou falha")
public record RunnerCompleteTaskRequest(
        @Schema(description = "Indica se a tarefa foi concluida com sucesso (true) ou falhou (false)",
                example = "true")
        boolean succeeded,
        @Schema(description = "Resumo estruturado do resultado da execucao",
                example = "Suite Maven concluida com sucesso: 106 testes aprovados em 14s.")
        @Size(max = 4000, message = "Resumo excede o limite")
        String resultSummary,
        @Schema(description = "Codigo de erro opcional quando succeeded for false", nullable = true)
        @Size(max = 64, message = "Codigo de erro excede o limite")
        String errorCode
) {
}
