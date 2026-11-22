package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo de uma execucao de planejamento do agente")
public record AgentRunSummaryResponse(
        @Schema(description = "Identificador da execucao do agente",
                example = "550e8400-e29b-41d4-a716-446655440040")
        String id,
        @Schema(description = "Identificador do projeto associado",
                example = "550e8400-e29b-41d4-a716-446655440010")
        String projectId,
        @Schema(description = "Objetivo solicitado pelo usuario",
                example = "Revisar a configuracao de CORS e propor teste unitario")
        String goal,
        @Schema(description = "Modelo de IA utilizado no planejamento", example = "z-ai/glm-5.3-flash")
        String modelId,
        @Schema(description = "Resumo estruturado do plano gerado pelo modelo",
                example = "Ler SecurityConfig.java e propor suite de validacao de origens permitidas")
        String planSummary,
        @Schema(description = "Estado atual (AWAITING_APPROVAL, READY_FOR_CLIENT, CANCELLED, FAILED)",
                example = "AWAITING_APPROVAL")
        String status,
        @Schema(description = "Quantidade de snapshots de arquivos enviados pelo cliente", example = "1")
        int workspaceFileCount,
        @Schema(description = "Quantidade de trechos RAG recuperados do projeto", example = "2")
        int ragMatchCount,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T18:15:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC da ultima atualizacao", example = "2026-11-22T18:15:02Z")
        Instant updatedAt
) {
}
