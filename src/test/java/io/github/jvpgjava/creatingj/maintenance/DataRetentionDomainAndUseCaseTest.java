package io.github.jvpgjava.creatingj.maintenance;

import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.maintenance.application.port.out.DataRetentionStorePort;
import io.github.jvpgjava.creatingj.maintenance.application.usecase.ExecuteDataRetentionInteractor;
import io.github.jvpgjava.creatingj.maintenance.domain.DataRetentionReport;
import io.github.jvpgjava.creatingj.maintenance.domain.RetentionPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataRetentionDomainAndUseCaseTest {

    @Test
    void shouldValidateRetentionPolicyAndRecordAuditOnExecution() {
        RetentionPolicy defaults = RetentionPolicy.defaults();
        assertThat(defaults.expiredSessionsDays()).isEqualTo(30);
        assertThat(defaults.auditEventsDays()).isEqualTo(180);

        assertThatThrownBy(() -> new RetentionPolicy(0, 7, 90, 90, 180))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> new RetentionPolicy(30, 0, 90, 90, 180)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new RetentionPolicy(30, 7, 0, 90, 180)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new RetentionPolicy(30, 7, 90, 0, 180)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new RetentionPolicy(30, 7, 90, 90, 6)).isInstanceOf(DomainException.class);

        DataRetentionStorePort store = new DataRetentionStorePort() {
            @Override
            public int deleteExpiredOrRevokedSessionsBefore(Instant cutoff) {
                return 3;
            }

            @Override
            public int deleteExpiredPairingChallengesBefore(Instant cutoff) {
                return 2;
            }

            @Override
            public int deleteTerminalTasksBefore(Instant cutoff) {
                return 4;
            }

            @Override
            public int deleteArchivedConversationsBefore(Instant cutoff) {
                return 1;
            }

            @Override
            public int deleteAuditEventsBefore(Instant cutoff) {
                return 5;
            }
        };

        List<AuditEvent> audits = new ArrayList<>();
        Instant now = Instant.parse("2026-11-22T16:15:00Z");
        ExecuteDataRetentionInteractor interactor = new ExecuteDataRetentionInteractor(
                store, defaults, audits::add, () -> now);

        DataRetentionReport report = interactor.execute(UserId.newId());
        assertThat(report.totalDeleted()).isEqualTo(15);
        assertThat(report.executedAt()).isEqualTo(now);
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).action()).isEqualTo(AuditAction.DATA_RETENTION_EXECUTED);

        DataRetentionReport systemReport = interactor.execute(null);
        assertThat(systemReport.totalDeleted()).isEqualTo(15);
        assertThat(audits.get(1).actorUserId()).isNull();
    }
}
