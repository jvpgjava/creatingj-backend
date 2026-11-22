package io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados completos de um projeto")
public record ProjectResponse(
        @Schema(description = "Identificador unico do projeto", example = "550e8400-e29b-41d4-a716-446655440010")
        String id,
        @Schema(description = "Nome do projeto", example = "CreatingJ Backend")
        String name,
        @Schema(description = "Slug unico do projeto no escopo do proprietario", example = "creatingj-backend")
        String slug,
        @Schema(description = "Descricao do projeto",
                example = "API hexagonal Java 25 com Spring Boot 4, pgvector e orquestracao de agentes")
        String description,
        @Schema(description = "Data de criacao em UTC", example = "2026-11-22T14:30:00Z")
        Instant createdAt,
        @Schema(description = "Data da ultima atualizacao em UTC", example = "2026-11-22T15:00:00Z")
        Instant updatedAt
) {
}
