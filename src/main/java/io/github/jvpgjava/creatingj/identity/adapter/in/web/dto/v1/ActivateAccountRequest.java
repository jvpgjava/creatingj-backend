package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para ativacao inicial de conta gerenciada com definicao de senha forte")
public record ActivateAccountRequest(
        @Schema(description = "Token de ativacao de uso unico emitido pelo administrador",
                example = "crj_act_4f8e1a2b3c4d5e6f7a8b9c0d1e2f3a4b")
        @NotBlank @Size(max = 256) String token,
        @Schema(description = "Senha forte (minimo 12 caracteres, com maiuscula, minuscula, numero e simbolo)",
                example = "CriarJ@2026#Forte")
        @NotBlank @Size(min = 12, max = 128) String password
) {
}
