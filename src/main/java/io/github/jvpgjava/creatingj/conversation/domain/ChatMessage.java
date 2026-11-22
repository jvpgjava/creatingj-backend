package io.github.jvpgjava.creatingj.conversation.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(
        UUID id,
        ConversationId conversationId,
        MessageRole role,
        String content,
        int sequenceNum,
        Instant createdAt
) {

    private static final int MAX_CONTENT_LENGTH = 16_000;

    public ChatMessage {
        if (id == null || conversationId == null || role == null || createdAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de mensagem ausentes.");
        }
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo da mensagem invalido.");
        }
        if (sequenceNum < 1) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "sequenceNum deve ser positivo.");
        }
    }

    public static ChatMessage create(
            UUID id,
            ConversationId conversationId,
            MessageRole role,
            String content,
            int sequenceNum,
            Instant createdAt
    ) {
        return new ChatMessage(id, conversationId, role, content, sequenceNum, createdAt);
    }
}
