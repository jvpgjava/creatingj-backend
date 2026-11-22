package io.github.jvpgjava.creatingj.inference.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.in.GetOwnUsageSummaryUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UsageSummary;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;

import java.time.Instant;
import java.util.List;

public final class GetOwnUsageSummaryInteractor implements GetOwnUsageSummaryUseCase {

    private static final int RECENT_LIMIT = 20;

    private final UserQuotaStorePort quotaStore;
    private final UsageRecordStorePort usageStore;
    private final ClockPort clock;
    private final long defaultDailyTokenLimit;
    private final int defaultDailyRequestLimit;
    private final int defaultMaxConcurrent;

    public GetOwnUsageSummaryInteractor(
            UserQuotaStorePort quotaStore,
            UsageRecordStorePort usageStore,
            ClockPort clock,
            long defaultDailyTokenLimit,
            int defaultDailyRequestLimit,
            int defaultMaxConcurrent
    ) {
        this.quotaStore = quotaStore;
        this.usageStore = usageStore;
        this.clock = clock;
        this.defaultDailyTokenLimit = defaultDailyTokenLimit;
        this.defaultDailyRequestLimit = defaultDailyRequestLimit;
        this.defaultMaxConcurrent = defaultMaxConcurrent;
    }

    @Override
    public UsageSummary execute(UserId actorUserId) {
        Instant now = clock.now();
        UtcDayWindow window = UtcDayWindow.containing(now);
        UserQuota quota = quotaStore.findByUserId(actorUserId)
                .orElseGet(() -> UserQuota.of(
                        actorUserId, defaultDailyTokenLimit, defaultDailyRequestLimit, defaultMaxConcurrent,
                        null, now));

        long usedTokens = usageStore.sumTokensByUserBetween(
                actorUserId, window.startInclusive(), window.endExclusive());
        long usedRequests = usageStore.countRequestsByUserBetween(
                actorUserId, window.startInclusive(), window.endExclusive());
        List<UsageRecord> recent = usageStore.findRecentByUser(actorUserId, RECENT_LIMIT);

        long remainingTokens = Math.max(0L, quota.dailyTokenLimit() - usedTokens);
        long remainingRequests = Math.max(0L, quota.dailyRequestLimit() - usedRequests);
        return new UsageSummary(
                window.startInclusive(),
                window.endExclusive(),
                usedTokens,
                usedRequests,
                quota,
                remainingTokens,
                remainingRequests,
                recent
        );
    }
}
