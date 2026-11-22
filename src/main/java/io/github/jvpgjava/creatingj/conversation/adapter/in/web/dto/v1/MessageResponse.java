package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import java.time.Instant;

public record MessageResponse(
        String id,
        String role,
        String content,
        int sequenceNum,
        Instant createdAt
) {
}
