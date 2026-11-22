package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PkceTokenRequest(
        @NotBlank @Size(max = 256) String code,
        @NotBlank @Size(max = 64) String clientId,
        @NotBlank @Size(max = 512) String redirectUri,
        @NotBlank @Size(min = 43, max = 128) String codeVerifier
) {
}
