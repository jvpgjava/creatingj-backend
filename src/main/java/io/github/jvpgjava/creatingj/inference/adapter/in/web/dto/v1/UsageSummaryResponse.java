package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import java.time.Instant;
import java.util.List;

public record UsageSummaryResponse(
        Instant windowStart,
        Instant windowEnd,
        long usedTokens,
        long usedRequests,
        long remainingTokens,
        long remainingRequests,
        UserQuotaResponse quota,
        List<UsageRecordResponse> recentRecords
) {
}
