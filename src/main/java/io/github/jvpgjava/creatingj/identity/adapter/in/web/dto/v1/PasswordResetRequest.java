package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para solicitar redefinicao de senha sem enumeracao de contas")
public record PasswordResetRequest(
        @Schema(description = "E-mail da conta para solicitacao de reset", example = "joao@creatingj.dev")
        @NotBlank @Email @Size(max = 254) String email
) {
}
