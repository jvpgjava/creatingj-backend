package io.github.jvpgjava.creatingj.conversation.domain;

import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;

import java.util.UUID;

public record GenerationStreamEvent(
        String eventId,
        String type,
        UUID generationId,
        GenerationStatus status,
        String delta,
        UUID assistantMessageId,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String errorCode
) {

    public static GenerationStreamEvent started(UUID generationId, int sequence) {
        return new GenerationStreamEvent(generationId + ":" + sequence, "generation.started", generationId,
                GenerationStatus.STREAMING, null, null, null, null, null, null);
    }

    public static GenerationStreamEvent delta(UUID generationId, int sequence, String delta) {
        return new GenerationStreamEvent(generationId + ":" + sequence, "generation.delta", generationId,
                GenerationStatus.STREAMING, delta, null, null, null, null, null);
    }

    public static GenerationStreamEvent completed(
            UUID generationId,
            int sequence,
            UUID assistantMessageId,
            TokenUsage usage
    ) {
        TokenUsage resolved = usage == null ? TokenUsage.empty() : usage;
        return new GenerationStreamEvent(generationId + ":" + sequence, "generation.completed", generationId,
                GenerationStatus.COMPLETED, null, assistantMessageId,
                resolved.promptTokens(), resolved.completionTokens(), resolved.totalTokens(), null);
    }

    public static GenerationStreamEvent cancelled(UUID generationId, int sequence) {
        return new GenerationStreamEvent(generationId + ":" + sequence, "generation.cancelled", generationId,
                GenerationStatus.CANCELLED, null, null, null, null, null, null);
    }

    public static GenerationStreamEvent failed(UUID generationId, int sequence, String errorCode) {
        return new GenerationStreamEvent(generationId + ":" + sequence, "generation.failed", generationId,
                GenerationStatus.FAILED, null, null, null, null, null, errorCode);
    }
}
