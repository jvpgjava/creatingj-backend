package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo de uma sessao ativa do usuario")
public record SessionSummaryResponse(
        @Schema(description = "Identificador da sessao", example = "550e8400-e29b-41d4-a716-446655440002")
        String id,
        @Schema(description = "Cliente vinculado a sessao", example = "creatingj-web")
        String clientId,
        @Schema(description = "Metodo de autenticacao (PASSWORD_BEARER, WEB_COOKIE, PKCE_REFRESH)",
                example = "WEB_COOKIE")
        String authMethod,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T18:00:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC de expiracao", example = "2026-11-23T06:00:00Z")
        Instant expiresAt
) {
}
