package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Historico completo de uma conversa com mensagens e geracoes")
public record ConversationHistoryResponse(
        @Schema(description = "Metadados da conversa")
        ConversationSummaryResponse conversation,
        @Schema(description = "Mensagens ordenadas por sequenceNum")
        List<MessageResponse> messages,
        @Schema(description = "Execucoes de geracao de resposta associadas a conversa")
        List<GenerationSummaryResponse> generations
) {
}
