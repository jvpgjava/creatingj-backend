package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;
import java.util.Set;

public record UserSummaryResponse(
        String id,
        String email,
        String displayName,
        String status,
        Set<String> roles,
        Instant createdAt,
        Instant suspendedAt
) {
}
