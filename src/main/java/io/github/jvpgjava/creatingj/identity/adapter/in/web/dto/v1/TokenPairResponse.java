package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Par de tokens emitido via PKCE ou rotacao de refresh token")
public record TokenPairResponse(
        @Schema(description = "Access token opaco de curta duracao",
                example = "crj_tok_8f9e0a1b2c3d4e5f6a7b8c9d0e1f2a3b")
        String accessToken,
        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,
        @Schema(description = "Instante UTC de expiracao do access token", example = "2026-11-22T18:15:00Z")
        Instant accessTokenExpiresAt,
        @Schema(description = "Refresh token opaco com rotacao obrigatoria a cada uso",
                example = "crj_ref_9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d")
        String refreshToken,
        @Schema(description = "Instante UTC de expiracao do refresh token", example = "2026-12-22T18:00:00Z")
        Instant refreshTokenExpiresAt,
        @Schema(description = "Identificador do usuario", example = "550e8400-e29b-41d4-a716-446655440001")
        String userId,
        @Schema(description = "E-mail do usuario", example = "joao@creatingj.dev")
        String email,
        @Schema(description = "Papeis do usuario", example = "[\"MEMBER\"]")
        Set<String> roles
) {
}
