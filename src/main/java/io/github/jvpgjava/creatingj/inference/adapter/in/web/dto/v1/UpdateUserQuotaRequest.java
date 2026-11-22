package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Configuracao de cotas diarias e concorrencia de inferencia de um usuario")
public record UpdateUserQuotaRequest(
        @Schema(description = "Limite diario de tokens (entrada + saida)", example = "500000")
        @Min(1) @Max(100_000_000L) long dailyTokenLimit,
        @Schema(description = "Limite diario de requisicoes de chat/agente", example = "300")
        @Min(1) @Max(100_000) int dailyRequestLimit,
        @Schema(description = "Maximo de geracoes simultaneas permitidas", example = "3")
        @Min(1) @Max(50) int maxConcurrentGenerations
) {
}
