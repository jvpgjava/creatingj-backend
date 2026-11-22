package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo de estado e conectividade de um dispositivo pareado")
public record DeviceSummaryResponse(
        @Schema(description = "Identificador do dispositivo", example = "550e8400-e29b-41d4-a716-446655440050")
        String id,
        @Schema(description = "Nome amigavel do dispositivo", example = "Workstation-Joao-Windows")
        String name,
        @Schema(description = "Sistema operacional (LINUX, WINDOWS, MACOS)", example = "WINDOWS")
        String platform,
        @Schema(description = "Estado cadastral (ACTIVE, REVOKED)", example = "ACTIVE")
        String status,
        @Schema(description = "Indica se houve heartbeat recente dentro da janela de presenca", example = "true")
        boolean online,
        @Schema(description = "Instante UTC do ultimo heartbeat recebido", example = "2026-11-22T18:20:00Z",
                nullable = true)
        Instant lastHeartbeatAt,
        @Schema(description = "Instante UTC de pareamento", example = "2026-11-22T18:00:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC de revogacao (nulo enquanto ativo)", nullable = true)
        Instant revokedAt
) {
}
