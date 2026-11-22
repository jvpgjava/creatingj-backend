package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.device.domain.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para gerar um codigo de pareamento temporario para um novo dispositivo")
public record CreatePairingChallengeRequest(
        @Schema(description = "Nome amigavel da maquina onde o agente local ira rodar",
                example = "Workstation-Joao-Windows")
        @NotBlank(message = "Nome do dispositivo obrigatorio")
        @Size(max = 120, message = "Nome do dispositivo deve ter no maximo 120 caracteres")
        String deviceName,
        @Schema(description = "Sistema operacional do dispositivo (LINUX, WINDOWS, MACOS)", example = "WINDOWS")
        @NotNull(message = "Plataforma obrigatoria")
        DevicePlatform platform
) {
}
