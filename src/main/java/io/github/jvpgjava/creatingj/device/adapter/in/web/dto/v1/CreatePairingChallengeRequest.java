package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.device.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePairingChallengeRequest(
        @NotBlank(message = "Nome do dispositivo obrigatorio")
        @Size(max = 120, message = "Nome do dispositivo deve ter no maximo 120 caracteres")
        String deviceName,
        @NotNull(message = "Plataforma obrigatoria")
        DevicePlatform platform
) {
}
