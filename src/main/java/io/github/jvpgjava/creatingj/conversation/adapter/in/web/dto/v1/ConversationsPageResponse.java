package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pagina de conversas pertencentes ao usuario autenticado")
public record ConversationsPageResponse(
        @Schema(description = "Lista de conversas na pagina atual")
        List<ConversationSummaryResponse> items,
        @Schema(description = "Indice da pagina (base 0)", example = "0")
        int page,
        @Schema(description = "Tamanho maximo da pagina", example = "20")
        int size,
        @Schema(description = "Quantidade total de conversas", example = "5")
        long totalElements
) {
}
