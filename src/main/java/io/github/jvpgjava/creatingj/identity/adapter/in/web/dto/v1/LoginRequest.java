package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais de login por e-mail e senha")
public record LoginRequest(
        @Schema(description = "E-mail cadastrado do usuario ativo", example = "joao@creatingj.dev")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Senha do usuario", example = "CriarJ@2026#Forte")
        @NotBlank @Size(max = 128) String password
) {
}
