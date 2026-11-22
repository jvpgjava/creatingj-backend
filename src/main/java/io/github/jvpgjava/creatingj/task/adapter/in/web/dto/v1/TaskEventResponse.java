package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Evento registrado na linha do tempo de execucao de uma tarefa remota")
public record TaskEventResponse(
        @Schema(description = "Identificador do evento", example = "550e8400-e29b-41d4-a716-446655440061")
        String id,
        @Schema(description = "Chave de deduplicacao do evento", example = "step-01-compile-ok")
        String eventKey,
        @Schema(description = "Tipo do evento (PROGRESS, APPROVAL_REQUESTED, APPROVAL_DECIDED, COMPLETED, FAILED)",
                example = "PROGRESS")
        String eventType,
        @Schema(description = "Descricao do evento",
                example = "Compilacao concluida sem erros; iniciando execucao dos testes unitarios.")
        String message,
        @Schema(description = "Instante UTC de registro", example = "2026-11-22T18:21:05Z")
        Instant createdAt
) {
}
