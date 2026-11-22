package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para trocar codigo PKCE + codeVerifier por access token e refresh token")
public record PkceTokenRequest(
        @Schema(description = "Codigo de autorizacao recebido em /api/v1/auth/pkce/authorize",
                example = "crj_code_3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c")
        @NotBlank @Size(max = 256) String code,
        @Schema(description = "Identificador do mesmo cliente usado na autorizacao", example = "creatingj-desktop")
        @NotBlank @Size(max = 64) String clientId,
        @Schema(description = "Mesma redirectUri informada na autorizacao", example = "creatingj://auth/callback")
        @NotBlank @Size(max = 512) String redirectUri,
        @Schema(description = "Segredo original gerado pelo cliente cujo SHA-256 Base64URL corresponde ao challenge",
                example = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        @NotBlank @Size(min = 43, max = 128) String codeVerifier
) {
}
