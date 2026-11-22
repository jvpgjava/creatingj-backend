package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Resposta do login por bearer token com dados basicos do usuario")
public record LoginResponse(
        @Schema(description = "Token de acesso opaco para envio em Authorization: Bearer <token>",
                example = "crj_tok_8f9e0a1b2c3d4e5f6a7b8c9d0e1f2a3b")
        String accessToken,
        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,
        @Schema(description = "Instante UTC de expiracao do token", example = "2026-11-23T06:00:00Z")
        Instant expiresAt,
        @Schema(description = "Identificador do usuario", example = "550e8400-e29b-41d4-a716-446655440001")
        String userId,
        @Schema(description = "E-mail autenticado", example = "joao@creatingj.dev")
        String email,
        @Schema(description = "Papeis atribuidos ao usuario", example = "[\"MEMBER\"]")
        Set<String> roles
) {
}
