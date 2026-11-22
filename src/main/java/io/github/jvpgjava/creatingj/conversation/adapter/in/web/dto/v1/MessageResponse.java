package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Mensagem persistida no historico da conversa")
public record MessageResponse(
        @Schema(description = "Identificador da mensagem", example = "550e8400-e29b-41d4-a716-446655440022")
        String id,
        @Schema(description = "Papel do autor (USER ou ASSISTANT)", example = "USER")
        String role,
        @Schema(description = "Conteudo textual da mensagem",
                example = "Explique como funciona a arquitetura hexagonal no backend CreatingJ.")
        String content,
        @Schema(description = "Numero de sequencia crescente na conversa", example = "1")
        int sequenceNum,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T18:05:10Z")
        Instant createdAt
) {
}
