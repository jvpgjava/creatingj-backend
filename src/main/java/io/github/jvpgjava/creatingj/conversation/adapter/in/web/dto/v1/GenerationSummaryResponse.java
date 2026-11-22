package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Metadados e consumo de tokens de uma geracao de resposta")
public record GenerationSummaryResponse(
        @Schema(description = "Identificador da geracao", example = "550e8400-e29b-41d4-a716-446655440021")
        String id,
        @Schema(description = "Modelo utilizado", example = "z-ai/glm-5.3-flash")
        String modelId,
        @Schema(description = "Estado da geracao (RUNNING, COMPLETED, CANCELLED, FAILED)", example = "COMPLETED")
        String status,
        @Schema(description = "Tokens consumidos no prompt", example = "120")
        Integer promptTokens,
        @Schema(description = "Tokens gerados na resposta", example = "185")
        Integer completionTokens,
        @Schema(description = "Total de tokens da geracao", example = "305")
        Integer totalTokens,
        @Schema(description = "Codigo de erro quando status for FAILED (nulo em sucesso)", nullable = true)
        String errorCode,
        @Schema(description = "Instante UTC de inicio", example = "2026-11-22T18:05:10Z")
        Instant createdAt,
        @Schema(description = "Instante UTC de conclusao", example = "2026-11-22T18:05:12Z")
        Instant completedAt
) {
}
