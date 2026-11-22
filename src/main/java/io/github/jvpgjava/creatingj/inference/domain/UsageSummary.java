package io.github.jvpgjava.creatingj.inference.domain;

import java.time.Instant;
import java.util.List;

public record UsageSummary(
        Instant windowStart,
        Instant windowEnd,
        long usedTokens,
        long usedRequests,
        UserQuota quota,
        long remainingTokens,
        long remainingRequests,
        List<UsageRecord> recentRecords
) {

    public UsageSummary {
        recentRecords = recentRecords == null ? List.of() : List.copyOf(recentRecords);
    }
}
