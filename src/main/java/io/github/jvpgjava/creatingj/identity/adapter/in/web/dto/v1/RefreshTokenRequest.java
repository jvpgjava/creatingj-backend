package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para rotacionar um refresh token valido (reuso revoga toda a familia de sessoes)")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token opaco vigente",
                example = "crj_ref_9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d")
        @NotBlank @Size(max = 256) String refreshToken
) {
}
