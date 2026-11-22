package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Registro individual de consumo de tokens e latencia de inferencia")
public record UsageRecordResponse(
        @Schema(description = "Identificador do registro de uso", example = "550e8400-e29b-41d4-a716-446655440090")
        String id,
        @Schema(description = "Identificador da conversa associada (quando aplicavel)",
                example = "550e8400-e29b-41d4-a716-446655440020")
        String conversationId,
        @Schema(description = "Identificador da geracao associada (quando aplicavel)",
                example = "550e8400-e29b-41d4-a716-446655440021")
        String generationId,
        @Schema(description = "Modelo utilizado pelo provedor", example = "z-ai/glm-5.3-flash")
        String modelId,
        @Schema(description = "Origem da chamada (CHAT_SYNC, CHAT_STREAM, AGENT_PLAN)", example = "CHAT_SYNC")
        String source,
        @Schema(description = "Tokens enviados no prompt", example = "180")
        int promptTokens,
        @Schema(description = "Tokens gerados na resposta", example = "125")
        int completionTokens,
        @Schema(description = "Total de tokens contabilizados", example = "305")
        int totalTokens,
        @Schema(description = "Indica se a contagem foi estimada localmente na ausencia de metadados do provedor",
                example = "false")
        boolean estimated,
        @Schema(description = "Latencia total da chamada ao provedor em milissegundos", example = "842")
        long latencyMs,
        @Schema(description = "Resultado final da chamada (COMPLETED, CANCELLED, FAILED)", example = "COMPLETED")
        String outcome,
        @Schema(description = "Instante UTC de gravacao do registro", example = "2026-11-22T18:05:12Z")
        Instant recordedAt
) {
}
