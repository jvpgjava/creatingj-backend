package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta generica da solicitacao de redefinicao de senha (anti-enumeracao)")
public record PasswordResetRequestedResponse(
        @Schema(description = "Mensagem neutra confirmando o recebimento da solicitacao",
                example = "Se existir uma conta ativa para este e-mail, as instrucoes de redefinicao foram geradas.")
        String message
) {
}
