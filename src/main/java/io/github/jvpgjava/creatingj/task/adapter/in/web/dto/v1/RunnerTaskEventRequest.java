package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Evento incremental de progresso reportado pelo runner com deduplicacao por eventKey")
public record RunnerTaskEventRequest(
        @Schema(description = "Chave unica do evento dentro da tarefa para evitar duplicidade em retentativas",
                example = "step-01-compile-ok")
        @NotBlank(message = "eventKey obrigatorio")
        @Size(max = 120, message = "eventKey deve ter no maximo 120 caracteres")
        String eventKey,
        @Schema(description = "Mensagem de progresso visivel para o usuario no dashboard/SSE",
                example = "Compilacao concluida sem erros; iniciando execucao dos testes unitarios.")
        @NotBlank(message = "Mensagem obrigatoria")
        @Size(max = 1000, message = "Mensagem deve ter no maximo 1000 caracteres")
        String message
) {
}
