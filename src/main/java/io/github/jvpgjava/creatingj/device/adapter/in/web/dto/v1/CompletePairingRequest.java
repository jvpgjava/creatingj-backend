package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePairingRequest(
        @NotBlank(message = "Codigo de pareamento obrigatorio")
        @Size(max = 200, message = "Codigo de pareamento invalido")
        String pairingCode
) {
}
