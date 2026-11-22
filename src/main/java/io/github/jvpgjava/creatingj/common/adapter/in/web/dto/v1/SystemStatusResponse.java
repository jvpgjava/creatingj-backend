package io.github.jvpgjava.creatingj.common.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Status basico de disponibilidade da API v1")
public record SystemStatusResponse(
        @Schema(description = "Nome do servico", example = "creatingj-backend")
        String service,
        @Schema(description = "Versao do contrato HTTP", example = "v1")
        String apiVersion,
        @Schema(description = "Estado atual da API", example = "UP")
        String status,
        @Schema(description = "Instante UTC da consulta", example = "2026-11-22T18:00:00Z")
        Instant timestamp
) {
}
