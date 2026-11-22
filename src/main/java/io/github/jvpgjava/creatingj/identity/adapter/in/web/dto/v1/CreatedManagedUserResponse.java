package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resposta da criacao administrativa de usuario contendo o token inicial de ativacao")
public record CreatedManagedUserResponse(
        @Schema(description = "Resumo do usuario provisionado")
        UserSummaryResponse user,
        @Schema(description = "Token de ativacao de uso unico (retornado apenas na criacao)",
                example = "crj_act_4f8e1a2b3c4d5e6f7a8b9c0d1e2f3a4b")
        String activationToken,
        @Schema(description = "Instante UTC de expiracao do token de ativacao", example = "2026-11-23T18:00:00Z")
        Instant activationExpiresAt
) {
}
