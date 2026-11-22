package io.github.jvpgjava.creatingj.conversation.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record ConversationId(UUID value) {

    public ConversationId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "conversationId obrigatorio.");
        }
    }

    public static ConversationId newId() {
        return new ConversationId(UUID.randomUUID());
    }

    public static ConversationId fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "conversationId invalido.");
        }
        try {
            return new ConversationId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "conversationId invalido.", ex);
        }
    }
}
