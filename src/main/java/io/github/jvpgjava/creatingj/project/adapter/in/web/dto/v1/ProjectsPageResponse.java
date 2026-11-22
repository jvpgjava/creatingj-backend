package io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pagina de projetos pertencentes ao usuario autenticado")
public record ProjectsPageResponse(
        @Schema(description = "Itens da pagina atual")
        List<ProjectResponse> items,
        @Schema(description = "Indice da pagina (base 0)", example = "0")
        int page,
        @Schema(description = "Tamanho maximo da pagina", example = "20")
        int size,
        @Schema(description = "Quantidade total de projetos encontrados", example = "3")
        long totalElements
) {
}
