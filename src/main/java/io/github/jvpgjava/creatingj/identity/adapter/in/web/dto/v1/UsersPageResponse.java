package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pagina administrativa de usuarios gerenciados")
public record UsersPageResponse(
        @Schema(description = "Usuarios retornados na pagina atual")
        List<UserSummaryResponse> items,
        @Schema(description = "Indice da pagina (base 0)", example = "0")
        int page,
        @Schema(description = "Tamanho maximo da pagina", example = "20")
        int size,
        @Schema(description = "Total de usuarios cadastrados", example = "12")
        long totalElements
) {
}
