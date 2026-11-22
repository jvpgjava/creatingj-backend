package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import java.time.Instant;

public record AdminUsageOverviewResponse(
        Instant windowStart,
        Instant windowEnd,
        long totalTokensToday,
        long totalRequestsToday
) {
}
