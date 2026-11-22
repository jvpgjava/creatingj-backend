package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

public record SseGenerationEventPayload(
        String generationId,
        String status,
        String delta,
        String assistantMessageId,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String errorCode
) {
}
