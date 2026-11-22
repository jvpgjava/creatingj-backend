package io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Nome do projeto obrigatorio")
        @Size(max = 120, message = "Nome do projeto deve ter no maximo 120 caracteres")
        String name,
        @Size(max = 120, message = "Slug do projeto deve ter no maximo 120 caracteres")
        String slug,
        @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
        String description
) {
}
