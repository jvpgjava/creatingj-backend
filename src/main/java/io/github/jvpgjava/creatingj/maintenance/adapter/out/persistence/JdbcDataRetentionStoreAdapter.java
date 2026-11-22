package io.github.jvpgjava.creatingj.maintenance.adapter.out.persistence;

import io.github.jvpgjava.creatingj.maintenance.application.port.out.DataRetentionStorePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcDataRetentionStoreAdapter implements DataRetentionStorePort {

    private final JdbcClient jdbcClient;

    public JdbcDataRetentionStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public int deleteExpiredOrRevokedSessionsBefore(Instant cutoff) {
        return jdbcClient.sql("""
                DELETE FROM sessions
                WHERE (expires_at < :cutoff)
                   OR (revoked_at IS NOT NULL AND revoked_at < :cutoff)
                """)
                .param("cutoff", toOffset(cutoff))
                .update();
    }

    @Override
    public int deleteExpiredPairingChallengesBefore(Instant cutoff) {
        return jdbcClient.sql("""
                DELETE FROM device_pairing_challenges
                WHERE expires_at < :cutoff
                """)
                .param("cutoff", toOffset(cutoff))
                .update();
    }

    @Override
    public int deleteTerminalTasksBefore(Instant cutoff) {
        return jdbcClient.sql("""
                DELETE FROM remote_tasks
                WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED')
                  AND updated_at < :cutoff
                """)
                .param("cutoff", toOffset(cutoff))
                .update();
    }

    @Override
    public int deleteArchivedConversationsBefore(Instant cutoff) {
        return jdbcClient.sql("""
                DELETE FROM conversations
                WHERE archived_at IS NOT NULL AND archived_at < :cutoff
                """)
                .param("cutoff", toOffset(cutoff))
                .update();
    }

    @Override
    public int deleteAuditEventsBefore(Instant cutoff) {
        return jdbcClient.sql("""
                DELETE FROM audit_events
                WHERE occurred_at < :cutoff
                """)
                .param("cutoff", toOffset(cutoff))
                .update();
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
