package io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criacao de um novo projeto do usuario")
public record CreateProjectRequest(
        @Schema(description = "Nome legivel do projeto", example = "CreatingJ Backend")
        @NotBlank(message = "Nome do projeto obrigatorio")
        @Size(max = 120, message = "Nome do projeto deve ter no maximo 120 caracteres")
        String name,
        @Schema(description = "Slug opcional em kebab-case (gerado automaticamente se omitido)",
                example = "creatingj-backend")
        @Size(max = 120, message = "Slug do projeto deve ter no maximo 120 caracteres")
        String slug,
        @Schema(description = "Descricao resumida do escopo do projeto",
                example = "API hexagonal Java 25 com Spring Boot 4, pgvector e orquestracao de agentes")
        @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
        String description
) {
}
