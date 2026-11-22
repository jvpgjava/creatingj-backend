package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo de estado e ciclo de vida de uma tarefa remota")
public record RemoteTaskSummaryResponse(
        @Schema(description = "Identificador da tarefa", example = "550e8400-e29b-41d4-a716-446655440060")
        String id,
        @Schema(description = "Identificador do projeto associado (opcional)",
                example = "550e8400-e29b-41d4-a716-446655440010", nullable = true)
        String projectId,
        @Schema(description = "Identificador do dispositivo executor",
                example = "550e8400-e29b-41d4-a716-446655440050")
        String deviceId,
        @Schema(description = "Titulo da tarefa", example = "Executar suite de testes Maven no projeto")
        String title,
        @Schema(description = "Instrucao completa da tarefa",
                example = "Rodar mvn -B test no repositorio creatingj-backend e reportar falhas encontradas.")
        String instruction,
        @Schema(description = "Estado atual (QUEUED, RUNNING, WAITING_APPROVAL, COMPLETED, FAILED, CANCELLED)",
                example = "RUNNING")
        String status,
        @Schema(description = "Instante UTC de expiracao do lease atual do runner",
                example = "2026-11-22T18:25:00Z", nullable = true)
        Instant leaseExpiresAt,
        @Schema(description = "Resumo final reportado pelo runner ao concluir",
                example = "106 testes executados com 0 falhas.", nullable = true)
        String resultSummary,
        @Schema(description = "Codigo de erro quando status for FAILED", nullable = true)
        String errorCode,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T18:20:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC da ultima atualizacao", example = "2026-11-22T18:21:00Z")
        Instant updatedAt,
        @Schema(description = "Instante UTC de conclusao (nulo enquanto em execucao)", nullable = true)
        Instant completedAt
) {
}
