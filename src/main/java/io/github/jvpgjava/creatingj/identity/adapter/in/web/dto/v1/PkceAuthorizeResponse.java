package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Codigo de autorizacao PKCE de uso unico emitido para troca por par de tokens")
public record PkceAuthorizeResponse(
        @Schema(description = "Codigo temporario de uso unico",
                example = "crj_code_3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c")
        String authorizationCode,
        @Schema(description = "URI de redirecionamento validada", example = "creatingj://auth/callback")
        String redirectUri,
        @Schema(description = "Valor state devolvido sem modificacao", example = "state-8f2a1c9d")
        String state,
        @Schema(description = "Instante UTC de expiracao do codigo", example = "2026-11-22T18:05:00Z")
        Instant expiresAt
) {
}
