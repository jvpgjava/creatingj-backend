package io.github.jvpgjava.creatingj.maintenance.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.maintenance.application.port.in.ExecuteDataRetentionUseCase;
import io.github.jvpgjava.creatingj.maintenance.application.port.out.DataRetentionStorePort;
import io.github.jvpgjava.creatingj.maintenance.domain.DataRetentionReport;
import io.github.jvpgjava.creatingj.maintenance.domain.RetentionPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class ExecuteDataRetentionInteractor implements ExecuteDataRetentionUseCase {

    private final DataRetentionStorePort retentionStore;
    private final RetentionPolicy policy;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public ExecuteDataRetentionInteractor(
            DataRetentionStorePort retentionStore,
            RetentionPolicy policy,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.retentionStore = retentionStore;
        this.policy = policy;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public DataRetentionReport execute(UserId actorUserId) {
        Instant now = clock.now();
        int sessions = retentionStore.deleteExpiredOrRevokedSessionsBefore(
                now.minus(Duration.ofDays(policy.expiredSessionsDays())));
        int challenges = retentionStore.deleteExpiredPairingChallengesBefore(
                now.minus(Duration.ofDays(policy.expiredPairingChallengesDays())));
        int tasks = retentionStore.deleteTerminalTasksBefore(
                now.minus(Duration.ofDays(policy.completedTasksDays())));
        int conversations = retentionStore.deleteArchivedConversationsBefore(
                now.minus(Duration.ofDays(policy.archivedConversationsDays())));
        int audits = retentionStore.deleteAuditEventsBefore(
                now.minus(Duration.ofDays(policy.auditEventsDays())));

        DataRetentionReport report = new DataRetentionReport(now, sessions, challenges, tasks, conversations, audits);
        UUID actorUuid = actorUserId == null ? null : actorUserId.value();
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                actorUuid,
                actorUuid,
                AuditAction.DATA_RETENTION_EXECUTED,
                "SUCCESS",
                "totalDeleted=" + report.totalDeleted()
                        + " sessions=" + sessions
                        + " challenges=" + challenges
                        + " tasks=" + tasks
                        + " conversations=" + conversations
                        + " audits=" + audits,
                now
        ));
        return report;
    }
}
