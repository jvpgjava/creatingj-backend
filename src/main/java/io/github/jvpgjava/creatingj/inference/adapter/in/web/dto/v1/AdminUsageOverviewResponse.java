package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Visao consolidada de consumo de inferencia na janela diaria atual")
public record AdminUsageOverviewResponse(
        @Schema(description = "Inicio da janela diaria em UTC", example = "2026-11-22T00:00:00Z")
        Instant windowStart,
        @Schema(description = "Fim da janela diaria em UTC", example = "2026-11-23T00:00:00Z")
        Instant windowEnd,
        @Schema(description = "Total de tokens consumidos hoje por todos os usuarios", example = "48250")
        long totalTokensToday,
        @Schema(description = "Total de requisicoes de inferencia executadas hoje", example = "134")
        long totalRequestsToday
) {
}
