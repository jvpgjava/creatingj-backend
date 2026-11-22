package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import java.time.Instant;

public record UsageRecordResponse(
        String id,
        String conversationId,
        String generationId,
        String modelId,
        String source,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        boolean estimated,
        long latencyMs,
        String outcome,
        Instant recordedAt
) {
}
