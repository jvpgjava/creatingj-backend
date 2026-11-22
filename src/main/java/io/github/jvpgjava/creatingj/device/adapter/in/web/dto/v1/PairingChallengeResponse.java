package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Desafio temporario de pareamento com codigo de uso unico")
public record PairingChallengeResponse(
        @Schema(description = "Identificador do desafio", example = "550e8400-e29b-41d4-a716-446655440051")
        String challengeId,
        @Schema(description = "Codigo de uso unico a ser inserido no agente local",
                example = "crj_pair_9f4c2a1b8e7d6c5a4b3c2d1e0f9a8b7c")
        String pairingCode,
        @Schema(description = "Nome do dispositivo associado ao desafio", example = "Workstation-Joao-Windows")
        String deviceName,
        @Schema(description = "Plataforma do dispositivo (LINUX, WINDOWS, MACOS)", example = "WINDOWS")
        String platform,
        @Schema(description = "Instante UTC de expiracao do codigo", example = "2026-11-22T18:10:00Z")
        Instant expiresAt
) {
}
