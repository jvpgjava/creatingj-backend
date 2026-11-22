package io.github.jvpgjava.creatingj.conversation.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;

public record Conversation(
        ConversationId id,
        UserId ownerUserId,
        String title,
        String modelId,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {

    private static final int MAX_TITLE_LENGTH = 160;

    public Conversation {
        if (id == null || ownerUserId == null || createdAt == null || updatedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de conversa ausentes.");
        }
        if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Titulo da conversa invalido.");
        }
        if (modelId == null || modelId.isBlank() || modelId.trim().length() > 120) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Modelo da conversa invalido.");
        }
        title = title.trim();
        modelId = modelId.trim();
    }

    public static Conversation create(
            ConversationId id,
            UserId ownerUserId,
            String title,
            String modelId,
            Instant now
    ) {
        return new Conversation(id, ownerUserId, title, modelId, now, now, null);
    }

    public boolean isOwnedBy(UserId candidateUserId) {
        return ownerUserId.equals(candidateUserId);
    }

    public Conversation touch(Instant now) {
        return new Conversation(id, ownerUserId, title, modelId, createdAt, now, archivedAt);
    }
}
