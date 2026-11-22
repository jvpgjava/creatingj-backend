package io.github.jvpgjava.creatingj.inference.domain;

import java.time.Instant;

public record AdminUsageOverview(
        Instant windowStart,
        Instant windowEnd,
        long totalTokensToday,
        long totalRequestsToday
) {
}
