package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Perfil e contexto de sessao do usuario autenticado")
public record CurrentUserResponse(
        @Schema(description = "Identificador do usuario", example = "550e8400-e29b-41d4-a716-446655440001")
        String id,
        @Schema(description = "E-mail do usuario", example = "joao@creatingj.dev")
        String email,
        @Schema(description = "Nome de exibicao", example = "Joao Grando")
        String displayName,
        @Schema(description = "Estado da conta (PENDING_ACTIVATION, ACTIVE, SUSPENDED)", example = "ACTIVE")
        String status,
        @Schema(description = "Papeis concedidos ao usuario", example = "[\"MEMBER\"]")
        Set<String> roles,
        @Schema(description = "Identificador da sessao atual", example = "550e8400-e29b-41d4-a716-446655440002")
        String sessionId,
        @Schema(description = "Identificador do cliente autenticado (creatingj-web, creatingj-desktop, etc.)",
                example = "creatingj-web")
        String clientId,
        @Schema(description = "Instante UTC de expiracao da sessao atual", example = "2026-11-23T06:00:00Z")
        Instant sessionExpiresAt
) {
}
