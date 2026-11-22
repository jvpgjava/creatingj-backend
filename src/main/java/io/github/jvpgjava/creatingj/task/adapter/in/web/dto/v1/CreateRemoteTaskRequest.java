package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criar e enfileirar uma tarefa remota em um dispositivo pareado")
public record CreateRemoteTaskRequest(
        @Schema(description = "Identificador do dispositivo de destino",
                example = "550e8400-e29b-41d4-a716-446655440050")
        @NotBlank(message = "Dispositivo de destino obrigatorio")
        String deviceId,
        @Schema(description = "Identificador opcional do projeto associado",
                example = "550e8400-e29b-41d4-a716-446655440010", nullable = true)
        String projectId,
        @Schema(description = "Chave opcional de idempotencia para evitar criacao duplicada em retentativas",
                example = "task-build-20261122-001")
        @Size(max = 120, message = "Chave de idempotencia deve ter no maximo 120 caracteres")
        String idempotencyKey,
        @Schema(description = "Titulo resumido da tarefa", example = "Executar suite de testes Maven no projeto")
        @NotBlank(message = "Titulo da tarefa obrigatorio")
        @Size(max = 160, message = "Titulo da tarefa deve ter no maximo 160 caracteres")
        String title,
        @Schema(description = "Instrucao detalhada que o agente local ira executar no workspace",
                example = "Rodar mvn -B test no repositorio creatingj-backend e reportar falhas encontradas.")
        @NotBlank(message = "Instrucao da tarefa obrigatoria")
        @Size(max = 4000, message = "Instrucao da tarefa deve ter no maximo 4000 caracteres")
        String instruction
) {
}
