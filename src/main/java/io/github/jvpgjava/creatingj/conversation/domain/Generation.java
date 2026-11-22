package io.github.jvpgjava.creatingj.conversation.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;

import java.time.Instant;
import java.util.UUID;

public record Generation(
        UUID id,
        ConversationId conversationId,
        UserId ownerUserId,
        UUID userMessageId,
        UUID assistantMessageId,
        String modelId,
        GenerationStatus status,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String errorCode,
        Instant createdAt,
        Instant completedAt
) {

    public Generation {
        if (id == null || conversationId == null || ownerUserId == null || userMessageId == null
                || status == null || createdAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de geracao ausentes.");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Modelo da geracao obrigatorio.");
        }
    }

    public static Generation streaming(
            UUID id,
            ConversationId conversationId,
            UserId ownerUserId,
            UUID userMessageId,
            String modelId,
            Instant now
    ) {
        return new Generation(id, conversationId, ownerUserId, userMessageId, null, modelId,
                GenerationStatus.STREAMING, null, null, null, null, now, null);
    }

    public Generation complete(UUID newAssistantMessageId, TokenUsage usage, Instant now) {
        TokenUsage resolved = usage == null ? TokenUsage.empty() : usage;
        return new Generation(id, conversationId, ownerUserId, userMessageId, newAssistantMessageId, modelId,
                GenerationStatus.COMPLETED, resolved.promptTokens(), resolved.completionTokens(),
                resolved.totalTokens(), null, createdAt, now);
    }

    public Generation cancel(UUID partialAssistantMessageId, Instant now) {
        return new Generation(id, conversationId, ownerUserId, userMessageId, partialAssistantMessageId, modelId,
                GenerationStatus.CANCELLED, promptTokens, completionTokens, totalTokens, null, createdAt, now);
    }

    public Generation fail(String newErrorCode, Instant now) {
        return new Generation(id, conversationId, ownerUserId, userMessageId, assistantMessageId, modelId,
                GenerationStatus.FAILED, promptTokens, completionTokens, totalTokens, newErrorCode, createdAt, now);
    }

    public boolean isTerminal() {
        return status == GenerationStatus.COMPLETED
                || status == GenerationStatus.CANCELLED
                || status == GenerationStatus.FAILED;
    }
}
