package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import java.time.Instant;

public record GenerationSummaryResponse(
        String id,
        String modelId,
        String status,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String errorCode,
        Instant createdAt,
        Instant completedAt
) {
}
