package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RunnerTaskEventRequest(
        @NotBlank(message = "eventKey obrigatorio")
        @Size(max = 120, message = "eventKey deve ter no maximo 120 caracteres")
        String eventKey,
        @NotBlank(message = "Mensagem obrigatoria")
        @Size(max = 1000, message = "Mensagem deve ter no maximo 1000 caracteres")
        String message
) {
}
