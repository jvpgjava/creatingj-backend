package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import java.time.Instant;

public record ConversationSummaryResponse(
        String id,
        String title,
        String modelId,
        Instant createdAt,
        Instant updatedAt
) {
}
