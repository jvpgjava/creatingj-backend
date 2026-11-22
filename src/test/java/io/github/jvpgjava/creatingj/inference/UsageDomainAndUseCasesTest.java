package io.github.jvpgjava.creatingj.inference;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase.RecordUsageCommand;
import io.github.jvpgjava.creatingj.inference.application.port.in.UpdateUserQuotaUseCase.UpdateUserQuotaCommand;
import io.github.jvpgjava.creatingj.inference.application.port.out.ConcurrencyLimitPort;
import io.github.jvpgjava.creatingj.inference.application.port.out.InferenceMetricsPort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.application.usecase.EnforceQuotaAndRecordUsageInteractor;
import io.github.jvpgjava.creatingj.inference.application.usecase.GetAdminUsageOverviewInteractor;
import io.github.jvpgjava.creatingj.inference.application.usecase.GetOwnUsageSummaryInteractor;
import io.github.jvpgjava.creatingj.inference.application.usecase.UpdateUserQuotaInteractor;
import io.github.jvpgjava.creatingj.inference.domain.TokenEstimator;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsageDomainAndUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-22T14:00:00Z");

    private ClockPort clock;
    private InMemoryUserStore userStore;
    private InMemoryQuotaStore quotaStore;
    private InMemoryUsageStore usageStore;
    private InMemoryConcurrencyLimiter concurrencyLimiter;
    private FakeMetrics metrics;
    private List<AuditEvent> auditEvents;
    private AuditPort auditPort;

    private User admin;
    private User member;

    private EnforceQuotaAndRecordUsageInteractor enforceAndRecord;
    private GetOwnUsageSummaryInteractor getOwnUsage;
    private GetAdminUsageOverviewInteractor getAdminOverview;
    private UpdateUserQuotaInteractor updateQuota;

    @BeforeEach
    void setUp() {
        clock = () -> NOW;
        userStore = new InMemoryUserStore();
        quotaStore = new InMemoryQuotaStore();
        usageStore = new InMemoryUsageStore();
        concurrencyLimiter = new InMemoryConcurrencyLimiter();
        metrics = new FakeMetrics();
        auditEvents = new ArrayList<>();
        auditPort = auditEvents::add;

        admin = User.active(UserId.newId(), Email.of("admin@creatingj.dev"), "Admin", "$argon2id$a",
                Set.of(Role.ADMIN), null, NOW);
        member = User.active(UserId.newId(), Email.of("member@creatingj.dev"), "Member", "$argon2id$m",
                Set.of(Role.MEMBER), admin.id(), NOW);
        userStore.save(admin);
        userStore.save(member);

        enforceAndRecord = new EnforceQuotaAndRecordUsageInteractor(
                quotaStore, usageStore, concurrencyLimiter, metrics, auditPort, clock, 100_000L, 200, 2);
        getOwnUsage = new GetOwnUsageSummaryInteractor(quotaStore, usageStore, clock, 100_000L, 200, 2);
        getAdminOverview = new GetAdminUsageOverviewInteractor(userStore, usageStore, clock);
        updateQuota = new UpdateUserQuotaInteractor(userStore, quotaStore, auditPort, clock);
    }

    @Test
    void shouldValidateQuotaUsageAndTokenEstimatorDomainRules() {
        assertThat(TokenEstimator.estimateTokens("")).isEqualTo(0);
        assertThat(TokenEstimator.estimateTokens(null)).isEqualTo(0);
        assertThat(TokenEstimator.estimateTokens("12345678")).isEqualTo(2);
        assertThat(TokenEstimator.estimateTokens("12345")).isEqualTo(2);

        UserQuota valid = UserQuota.of(member.id(), 50_000L, 100, 3, admin.id(), NOW);
        assertThat(valid.dailyTokenLimit()).isEqualTo(50_000L);

        assertThatThrownBy(() -> UserQuota.of(null, 100L, 10, 1, null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserQuota.of(member.id(), 0L, 10, 1, null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserQuota.of(member.id(), 100L, 0, 1, null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserQuota.of(member.id(), 100L, 10, 0, null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserQuota.of(member.id(), 100L, 10, 51, null, NOW))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> new UsageRecord(null, member.id(), null, null, "m", UsageSource.CHAT_SYNC,
                1, 1, 2, false, 10, UsageOutcome.COMPLETED, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new UsageRecord(UUID.randomUUID(), member.id(), null, null, " ",
                UsageSource.CHAT_SYNC, 1, 1, 2, false, 10, UsageOutcome.COMPLETED, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new UsageRecord(UUID.randomUUID(), member.id(), null, null, "m",
                UsageSource.CHAT_SYNC, -1, 1, 2, false, 10, UsageOutcome.COMPLETED, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new UsageRecord(UUID.randomUUID(), member.id(), null, null, "m",
                UsageSource.CHAT_SYNC, 1, 1, 2, false, -5, UsageOutcome.COMPLETED, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldEnforceTokenRequestAndConcurrencyQuotasAndRecordEstimatedOrRealUsage() {
        updateQuota.execute(new UpdateUserQuotaCommand(admin.id(), member.id(), 100L, 2, 1));
        assertThat(auditEvents).anyMatch(e -> e.action() == AuditAction.USER_QUOTA_UPDATED);

        enforceAndRecord.acquirePermit(member.id());
        assertThatThrownBy(() -> enforceAndRecord.acquirePermit(member.id()))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENCY_LIMIT_EXCEEDED);
        assertThat(metrics.rejectedReasons).contains("CONCURRENCY");

        enforceAndRecord.recordAndRelease(new RecordUsageCommand(
                member.id(), UUID.randomUUID(), UUID.randomUUID(), "z-ai/glm-5.3-flash",
                UsageSource.CHAT_SYNC, new TokenUsage(0, 0, 0), 400, 400, 120L, UsageOutcome.COMPLETED));

        var summary = getOwnUsage.execute(member.id());
        assertThat(summary.usedRequests()).isEqualTo(1);
        assertThat(summary.usedTokens()).isEqualTo(200);
        assertThat(summary.remainingTokens()).isEqualTo(0);
        assertThat(summary.recentRecords()).hasSize(1);
        assertThat(summary.recentRecords().get(0).estimated()).isTrue();

        assertThatThrownBy(() -> enforceAndRecord.acquirePermit(member.id()))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUOTA_EXCEEDED);
        assertThat(metrics.rejectedReasons).contains("DAILY_TOKENS");

        updateQuota.execute(new UpdateUserQuotaCommand(admin.id(), member.id(), 10_000L, 1, 2));
        assertThatThrownBy(() -> enforceAndRecord.acquirePermit(member.id()))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUOTA_EXCEEDED);
        assertThat(metrics.rejectedReasons).contains("DAILY_REQUESTS");
    }

    @Test
    void shouldProtectAdminUsageAndQuotaEndpointsFromNonAdmins() {
        enforceAndRecord.acquirePermit(member.id());
        enforceAndRecord.recordAndRelease(new RecordUsageCommand(
                member.id(), UUID.randomUUID(), UUID.randomUUID(), "z-ai/glm-5.3-flash",
                UsageSource.CHAT_STREAM, new TokenUsage(10, 20, 30), 40, 80, 250L, UsageOutcome.COMPLETED));

        var overview = getAdminOverview.execute(admin.id());
        assertThat(overview.totalTokensToday()).isEqualTo(30L);
        assertThat(overview.totalRequestsToday()).isEqualTo(1L);

        assertThatThrownBy(() -> getAdminOverview.execute(member.id()))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> updateQuota.execute(
                new UpdateUserQuotaCommand(member.id(), member.id(), 1000L, 10, 1)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> updateQuota.execute(
                new UpdateUserQuotaCommand(admin.id(), UserId.newId(), 1000L, 10, 1)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private static final class InMemoryUserStore implements UserStorePort {
        private final Map<UserId, User> map = new HashMap<>();

        @Override
        public User save(User user) {
            map.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UserId id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return map.values().stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public List<User> findPage(int page, int size, AccountStatus status, UserSortField sortBy, boolean ascending) {
            return List.copyOf(map.values());
        }

        @Override
        public long count(AccountStatus status) {
            return map.size();
        }
    }

    private static final class InMemoryQuotaStore implements UserQuotaStorePort {
        private final Map<UserId, UserQuota> map = new HashMap<>();

        @Override
        public Optional<UserQuota> findByUserId(UserId userId) {
            return Optional.ofNullable(map.get(userId));
        }

        @Override
        public UserQuota save(UserQuota quota) {
            map.put(quota.userId(), quota);
            return quota;
        }
    }

    private static final class InMemoryUsageStore implements UsageRecordStorePort {
        private final List<UsageRecord> records = new ArrayList<>();

        @Override
        public UsageRecord save(UsageRecord record) {
            records.add(record);
            return record;
        }

        @Override
        public long sumTokensByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive) {
            return records.stream()
                    .filter(r -> r.userId().equals(userId)
                            && !r.recordedAt().isBefore(startInclusive)
                            && r.recordedAt().isBefore(endExclusive))
                    .mapToLong(UsageRecord::totalTokens)
                    .sum();
        }

        @Override
        public long countRequestsByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive) {
            return records.stream()
                    .filter(r -> r.userId().equals(userId)
                            && !r.recordedAt().isBefore(startInclusive)
                            && r.recordedAt().isBefore(endExclusive))
                    .count();
        }

        @Override
        public List<UsageRecord> findRecentByUser(UserId userId, int limit) {
            return records.stream()
                    .filter(r -> r.userId().equals(userId))
                    .sorted(Comparator.comparing(UsageRecord::recordedAt).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public long sumAllTokensBetween(Instant startInclusive, Instant endExclusive) {
            return records.stream()
                    .filter(r -> !r.recordedAt().isBefore(startInclusive) && r.recordedAt().isBefore(endExclusive))
                    .mapToLong(UsageRecord::totalTokens)
                    .sum();
        }

        @Override
        public long countAllRequestsBetween(Instant startInclusive, Instant endExclusive) {
            return records.stream()
                    .filter(r -> !r.recordedAt().isBefore(startInclusive) && r.recordedAt().isBefore(endExclusive))
                    .count();
        }
    }

    private static final class InMemoryConcurrencyLimiter implements ConcurrencyLimitPort {
        private final Map<UserId, Integer> active = new HashMap<>();

        @Override
        public boolean tryAcquire(UserId userId, int maxConcurrent) {
            int current = active.getOrDefault(userId, 0);
            if (current >= maxConcurrent) {
                return false;
            }
            active.put(userId, current + 1);
            return true;
        }

        @Override
        public void release(UserId userId) {
            active.computeIfPresent(userId, (k, v) -> v <= 1 ? null : v - 1);
        }
    }

    private static final class FakeMetrics implements InferenceMetricsPort {
        private final List<String> rejectedReasons = new ArrayList<>();

        @Override
        public void recordGeneration(
                String modelId,
                UsageSource source,
                UsageOutcome outcome,
                long latencyMs,
                int totalTokens
        ) {
        }

        @Override
        public void recordQuotaRejected(String reason) {
            rejectedReasons.add(reason);
        }
    }
}
