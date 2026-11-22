package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;

public record SessionSummaryResponse(
        String id,
        String clientId,
        String authMethod,
        Instant createdAt,
        Instant expiresAt
) {
}
