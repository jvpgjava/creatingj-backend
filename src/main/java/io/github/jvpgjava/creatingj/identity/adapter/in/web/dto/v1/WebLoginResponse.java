package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Resposta do login web apos emitir os cookies CREATINGJ_SESSION (HttpOnly) e XSRF-TOKEN")
public record WebLoginResponse(
        @Schema(description = "Identificador do usuario autenticado", example = "550e8400-e29b-41d4-a716-446655440001")
        String userId,
        @Schema(description = "E-mail do usuario autenticado", example = "joao@creatingj.dev")
        String email,
        @Schema(description = "Papeis concedidos ao usuario", example = "[\"MEMBER\"]")
        Set<String> roles,
        @Schema(description = "Instante UTC de expiracao da sessao web", example = "2026-11-23T06:00:00Z")
        Instant sessionExpiresAt
) {
}
