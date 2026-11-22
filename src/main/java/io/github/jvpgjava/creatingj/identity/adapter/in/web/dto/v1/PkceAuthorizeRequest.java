package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitacao de codigo de autorizacao OAuth 2.0 com PKCE (S256) para clientes nativos")
public record PkceAuthorizeRequest(
        @Schema(description = "Identificador do cliente permitido (creatingj-desktop, creatingj-vscode, "
                + "creatingj-mobile)", example = "creatingj-desktop")
        @NotBlank @Size(max = 64) String clientId,
        @Schema(description = "URI de redirecionamento registrada para o cliente",
                example = "creatingj://auth/callback")
        @NotBlank @Size(max = 512) String redirectUri,
        @Schema(description = "Hash SHA-256 em Base64URL do codeVerifier (43..128 caracteres)",
                example = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
        @NotBlank @Size(min = 43, max = 128) String codeChallenge,
        @Schema(description = "Metodo de desafio PKCE (apenas S256 e aceito)", example = "S256")
        @NotBlank @Size(max = 16) String codeChallengeMethod,
        @Schema(description = "Valor opaco opcional para correlacao anti-CSRF no cliente",
                example = "state-8f2a1c9d")
        @Size(max = 256) String state
) {
}
