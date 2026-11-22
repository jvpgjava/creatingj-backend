package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRemoteTaskRequest(
        @NotBlank(message = "Dispositivo de destino obrigatorio")
        String deviceId,
        String projectId,
        @Size(max = 120, message = "Chave de idempotencia deve ter no maximo 120 caracteres")
        String idempotencyKey,
        @NotBlank(message = "Titulo da tarefa obrigatorio")
        @Size(max = 160, message = "Titulo da tarefa deve ter no maximo 160 caracteres")
        String title,
        @NotBlank(message = "Instrucao da tarefa obrigatoria")
        @Size(max = 4000, message = "Instrucao da tarefa deve ter no maximo 4000 caracteres")
        String instruction
) {
}
