package io.github.jvpgjava.creatingj.inference.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.ConcurrencyLimitPort;
import io.github.jvpgjava.creatingj.inference.application.port.out.InferenceMetricsPort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.domain.TokenEstimator;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;

import java.time.Instant;
import java.util.UUID;

public final class EnforceQuotaAndRecordUsageInteractor implements EnforceQuotaAndRecordUsageUseCase {

    private final UserQuotaStorePort quotaStore;
    private final UsageRecordStorePort usageStore;
    private final ConcurrencyLimitPort concurrencyLimiter;
    private final InferenceMetricsPort metrics;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final long defaultDailyTokenLimit;
    private final int defaultDailyRequestLimit;
    private final int defaultMaxConcurrent;

    public EnforceQuotaAndRecordUsageInteractor(
            UserQuotaStorePort quotaStore,
            UsageRecordStorePort usageStore,
            ConcurrencyLimitPort concurrencyLimiter,
            InferenceMetricsPort metrics,
            AuditPort auditPort,
            ClockPort clock,
            long defaultDailyTokenLimit,
            int defaultDailyRequestLimit,
            int defaultMaxConcurrent
    ) {
        this.quotaStore = quotaStore;
        this.usageStore = usageStore;
        this.concurrencyLimiter = concurrencyLimiter;
        this.metrics = metrics;
        this.auditPort = auditPort;
        this.clock = clock;
        this.defaultDailyTokenLimit = defaultDailyTokenLimit;
        this.defaultDailyRequestLimit = defaultDailyRequestLimit;
        this.defaultMaxConcurrent = defaultMaxConcurrent;
    }

    @Override
    public void acquirePermit(UserId userId) {
        Instant now = clock.now();
        UtcDayWindow window = UtcDayWindow.containing(now);
        UserQuota quota = quotaStore.findByUserId(userId)
                .orElseGet(() -> UserQuota.of(
                        userId, defaultDailyTokenLimit, defaultDailyRequestLimit, defaultMaxConcurrent, null, now));

        long usedTokens = usageStore.sumTokensByUserBetween(userId, window.startInclusive(), window.endExclusive());
        if (usedTokens >= quota.dailyTokenLimit()) {
            rejectQuota(userId, "DAILY_TOKENS", now,
                    ErrorCode.QUOTA_EXCEEDED, "Cota diaria de tokens excedida.");
        }

        long usedRequests = usageStore.countRequestsByUserBetween(
                userId, window.startInclusive(), window.endExclusive());
        if (usedRequests >= quota.dailyRequestLimit()) {
            rejectQuota(userId, "DAILY_REQUESTS", now,
                    ErrorCode.QUOTA_EXCEEDED, "Cota diaria de requisicoes excedida.");
        }

        if (!concurrencyLimiter.tryAcquire(userId, quota.maxConcurrentGenerations())) {
            rejectQuota(userId, "CONCURRENCY", now,
                    ErrorCode.CONCURRENCY_LIMIT_EXCEEDED, "Limite de geracoes simultaneas excedido.");
        }
    }

    @Override
    public UsageRecord recordAndRelease(RecordUsageCommand command) {
        try {
            Instant now = clock.now();
            TokenUsage reported = command.reportedUsage();
            boolean estimated = reported == null || reported.totalTokens() <= 0;
            int promptTokens = estimated
                    ? TokenEstimator.estimateFromChars(command.promptCharCount())
                    : Math.max(0, reported.promptTokens());
            int completionTokens = estimated
                    ? TokenEstimator.estimateFromChars(command.completionCharCount())
                    : Math.max(0, reported.completionTokens());
            int totalTokens = estimated
                    ? promptTokens + completionTokens
                    : Math.max(reported.totalTokens(), promptTokens + completionTokens);

            long safeLatency = Math.max(0L, command.latencyMs());
            UsageRecord saved = usageStore.save(new UsageRecord(
                    UUID.randomUUID(),
                    command.userId(),
                    command.conversationId(),
                    command.generationId(),
                    command.modelId(),
                    command.source(),
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    estimated,
                    safeLatency,
                    command.outcome(),
                    now
            ));

            metrics.recordGeneration(
                    command.modelId(),
                    command.source(),
                    command.outcome(),
                    safeLatency,
                    totalTokens
            );
            return saved;
        } finally {
            concurrencyLimiter.release(command.userId());
        }
    }

    private void rejectQuota(UserId userId, String reason, Instant now, ErrorCode code, String detail) {
        metrics.recordQuotaRejected(reason);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                userId.value(),
                userId.value(),
                AuditAction.QUOTA_EXCEEDED,
                "REJECTED",
                "Solicitacao rejeitada por limite operacional: " + reason,
                now
        ));
        throw new DomainException(code, detail);
    }
}
