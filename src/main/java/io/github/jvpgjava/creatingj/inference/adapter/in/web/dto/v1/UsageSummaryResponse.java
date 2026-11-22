package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Resumo diario de consumo, saldo restante e historico recente do usuario")
public record UsageSummaryResponse(
        @Schema(description = "Inicio da janela diaria em UTC", example = "2026-11-22T00:00:00Z")
        Instant windowStart,
        @Schema(description = "Fim da janela diaria em UTC", example = "2026-11-23T00:00:00Z")
        Instant windowEnd,
        @Schema(description = "Tokens consumidos na janela atual", example = "12450")
        long usedTokens,
        @Schema(description = "Requisicoes realizadas na janela atual", example = "28")
        long usedRequests,
        @Schema(description = "Saldo restante de tokens na janela atual", example = "487550")
        long remainingTokens,
        @Schema(description = "Saldo restante de requisicoes na janela atual", example = "272")
        long remainingRequests,
        @Schema(description = "Politica de cota ativa para o usuario")
        UserQuotaResponse quota,
        @Schema(description = "Lista dos registros de consumo mais recentes")
        List<UsageRecordResponse> recentRecords
) {
}
