package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Cota vigente de consumo e concorrencia de um usuario")
public record UserQuotaResponse(
        @Schema(description = "Identificador do usuario", example = "550e8400-e29b-41d4-a716-446655440001")
        String userId,
        @Schema(description = "Limite diario de tokens", example = "500000")
        long dailyTokenLimit,
        @Schema(description = "Limite diario de requisicoes", example = "300")
        int dailyRequestLimit,
        @Schema(description = "Maximo de geracoes simultaneas", example = "3")
        int maxConcurrentGenerations,
        @Schema(description = "Instante UTC da ultima atualizacao da cota", example = "2026-11-22T16:00:00Z")
        Instant updatedAt
) {
}
