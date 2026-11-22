package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.in.GetAdminUsageOverviewUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.in.GetOwnUsageSummaryUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.in.UpdateUserQuotaUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.ConcurrencyLimitPort;
import io.github.jvpgjava.creatingj.inference.application.port.out.InferenceMetricsPort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.application.usecase.EnforceQuotaAndRecordUsageInteractor;
import io.github.jvpgjava.creatingj.inference.application.usecase.GetAdminUsageOverviewInteractor;
import io.github.jvpgjava.creatingj.inference.application.usecase.GetOwnUsageSummaryInteractor;
import io.github.jvpgjava.creatingj.inference.application.usecase.UpdateUserQuotaInteractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InferenceBeansConfig {

    @Bean
    public EnforceQuotaAndRecordUsageUseCase enforceQuotaAndRecordUsageUseCase(
            UserQuotaStorePort quotaStore,
            UsageRecordStorePort usageStore,
            ConcurrencyLimitPort concurrencyLimiter,
            InferenceMetricsPort metrics,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.quota.default-daily-tokens:10000000}") long defaultDailyTokenLimit,
            @Value("${creatingj.quota.default-daily-requests:500}") int defaultDailyRequestLimit,
            @Value("${creatingj.quota.default-max-concurrent:4}") int defaultMaxConcurrent
    ) {
        return new EnforceQuotaAndRecordUsageInteractor(
                quotaStore, usageStore, concurrencyLimiter, metrics, auditPort, clockPort,
                defaultDailyTokenLimit, defaultDailyRequestLimit, defaultMaxConcurrent);
    }

    @Bean
    public GetOwnUsageSummaryUseCase getOwnUsageSummaryUseCase(
            UserQuotaStorePort quotaStore,
            UsageRecordStorePort usageStore,
            ClockPort clockPort,
            @Value("${creatingj.quota.default-daily-tokens:10000000}") long defaultDailyTokenLimit,
            @Value("${creatingj.quota.default-daily-requests:500}") int defaultDailyRequestLimit,
            @Value("${creatingj.quota.default-max-concurrent:4}") int defaultMaxConcurrent
    ) {
        return new GetOwnUsageSummaryInteractor(
                quotaStore, usageStore, clockPort,
                defaultDailyTokenLimit, defaultDailyRequestLimit, defaultMaxConcurrent);
    }

    @Bean
    public GetAdminUsageOverviewUseCase getAdminUsageOverviewUseCase(
            UserStorePort userStore,
            UsageRecordStorePort usageStore,
            ClockPort clockPort
    ) {
        return new GetAdminUsageOverviewInteractor(userStore, usageStore, clockPort);
    }

    @Bean
    public UpdateUserQuotaUseCase updateUserQuotaUseCase(
            UserStorePort userStore,
            UserQuotaStorePort quotaStore,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new UpdateUserQuotaInteractor(userStore, quotaStore, auditPort, clockPort);
    }
}
