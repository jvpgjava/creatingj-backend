package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import java.time.Instant;

public record TaskEventResponse(
        String id,
        String eventKey,
        String eventType,
        String message,
        Instant createdAt
) {
}
