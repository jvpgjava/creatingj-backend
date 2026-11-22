package io.github.jvpgjava.creatingj.inference.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.UUID;

public record UsageRecord(
        UUID id,
        UserId userId,
        UUID conversationId,
        UUID generationId,
        String modelId,
        UsageSource source,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        boolean estimated,
        long latencyMs,
        UsageOutcome outcome,
        Instant recordedAt
) {

    public UsageRecord {
        if (id == null || userId == null || source == null || outcome == null || recordedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de consumo ausentes.");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "modelId obrigatorio no registro de consumo.");
        }
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0 || latencyMs < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Metricas de consumo nao podem ser negativas.");
        }
    }
}
