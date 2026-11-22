package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import java.time.Instant;

public record UserQuotaResponse(
        String userId,
        long dailyTokenLimit,
        int dailyRequestLimit,
        int maxConcurrentGenerations,
        Instant updatedAt
) {
}
