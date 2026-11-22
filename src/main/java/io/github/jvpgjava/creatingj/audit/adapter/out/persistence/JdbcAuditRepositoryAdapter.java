package io.github.jvpgjava.creatingj.audit.adapter.out.persistence;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuditRepositoryAdapter implements AuditPort {

    private final JdbcClient jdbcClient;

    public JdbcAuditRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void record(AuditEvent event) {
        jdbcClient.sql("""
                        INSERT INTO audit_events
                            (id, actor_user_id, target_user_id, action, outcome, summary, occurred_at)
                        VALUES
                            (:id, :actorUserId, :targetUserId, :action, :outcome, :summary, :occurredAt)
                        """)
                .param("id", event.id())
                .param("actorUserId", event.actorUserId())
                .param("targetUserId", event.targetUserId())
                .param("action", event.action().name())
                .param("outcome", event.outcome())
                .param("summary", event.summary())
                .param("occurredAt", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .update();
    }
}
