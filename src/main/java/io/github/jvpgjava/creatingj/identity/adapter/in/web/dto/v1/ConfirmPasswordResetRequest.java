package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para concluir a redefinicao de senha usando token de uso unico")
public record ConfirmPasswordResetRequest(
        @Schema(description = "Token de redefinicao de senha de uso unico",
                example = "crj_rst_1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d")
        @NotBlank @Size(max = 256) String token,
        @Schema(description = "Nova senha forte (minimo 12 caracteres)", example = "NovaSenha@2026#Segura")
        @NotBlank @Size(min = 12, max = 128) String newPassword
) {
}
