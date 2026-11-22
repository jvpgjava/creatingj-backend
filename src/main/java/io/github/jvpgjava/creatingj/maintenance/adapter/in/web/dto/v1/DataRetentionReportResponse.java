package io.github.jvpgjava.creatingj.maintenance.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Relatorio da execucao da politica de retencao e expurgo de dados")
public record DataRetentionReportResponse(
        @Schema(description = "Instante UTC da execucao", example = "2026-11-22T18:00:00Z")
        Instant executedAt,
        @Schema(description = "Sessoes expiradas removidas", example = "12")
        int deletedSessions,
        @Schema(description = "Desafios de pareamento expirados removidos", example = "3")
        int deletedPairingChallenges,
        @Schema(description = "Tarefas concluidas antigas removidas", example = "8")
        int deletedCompletedTasks,
        @Schema(description = "Conversas arquivadas antigas removidas", example = "4")
        int deletedArchivedConversations,
        @Schema(description = "Eventos de auditoria antigos removidos", example = "45")
        int deletedAuditEvents,
        @Schema(description = "Total de registros removidos na execucao", example = "72")
        int totalDeleted
) {
}
