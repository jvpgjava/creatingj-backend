package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo de uma conversa de chat")
public record ConversationSummaryResponse(
        @Schema(description = "Identificador da conversa", example = "550e8400-e29b-41d4-a716-446655440020")
        String id,
        @Schema(description = "Titulo da conversa", example = "Duvidas sobre arquitetura hexagonal")
        String title,
        @Schema(description = "Modelo de IA configurado para a conversa", example = "z-ai/glm-5.3-flash")
        String modelId,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T18:00:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC da ultima interacao", example = "2026-11-22T18:05:12Z")
        Instant updatedAt
) {
}
