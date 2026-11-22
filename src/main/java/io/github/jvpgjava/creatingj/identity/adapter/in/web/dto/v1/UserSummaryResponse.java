package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Resumo cadastral de um usuario gerenciado")
public record UserSummaryResponse(
        @Schema(description = "Identificador do usuario", example = "550e8400-e29b-41d4-a716-446655440001")
        String id,
        @Schema(description = "E-mail do usuario", example = "joao@creatingj.dev")
        String email,
        @Schema(description = "Nome de exibicao", example = "Joao Grando")
        String displayName,
        @Schema(description = "Estado da conta (PENDING_ACTIVATION, ACTIVE, SUSPENDED)", example = "ACTIVE")
        String status,
        @Schema(description = "Papeis atribuidos", example = "[\"MEMBER\"]")
        Set<String> roles,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T14:00:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC de suspensao (nulo quando nao suspenso)", nullable = true)
        Instant suspendedAt
) {
}
