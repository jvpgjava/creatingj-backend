package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordResetRequest(
        @NotBlank @Size(max = 256) String token,
        @NotBlank @Size(min = 12, max = 128) String newPassword
) {
}
