package io.github.jvpgjava.creatingj.identity.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcInvitationStoreAdapter implements InvitationStorePort {

    private final JdbcClient jdbcClient;

    public JdbcInvitationStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Invitation save(Invitation invitation) {
        jdbcClient.sql("""
                        INSERT INTO invitations
                            (id, user_id, purpose, token_hash, expires_at, used_at, created_at)
                        VALUES
                            (:id, :userId, :purpose, :tokenHash, :expiresAt, :usedAt, :createdAt)
                        ON CONFLICT (id) DO UPDATE SET
                            used_at = EXCLUDED.used_at
                        """)
                .param("id", invitation.id())
                .param("userId", invitation.userId().value())
                .param("purpose", invitation.purpose().name())
                .param("tokenHash", invitation.tokenHash())
                .param("expiresAt", toOffset(invitation.expiresAt()))
                .param("usedAt", toOffset(invitation.usedAt()))
                .param("createdAt", toOffset(invitation.createdAt()))
                .update();
        return invitation;
    }

    @Override
    public Optional<Invitation> findByTokenHash(String tokenHash) {
        return jdbcClient.sql("""
                        SELECT id, user_id, purpose, token_hash, expires_at, used_at, created_at
                        FROM invitations
                        WHERE token_hash = :tokenHash
                        """)
                .param("tokenHash", tokenHash)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public void invalidatePendingByUserAndPurpose(UserId userId, InvitationPurpose purpose, Instant now) {
        jdbcClient.sql("""
                        UPDATE invitations
                        SET used_at = :now
                        WHERE user_id = :userId
                          AND purpose = :purpose
                          AND used_at IS NULL
                        """)
                .param("now", toOffset(now))
                .param("userId", userId.value())
                .param("purpose", purpose.name())
                .update();
    }

    private Invitation mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime usedAt = rs.getObject("used_at", OffsetDateTime.class);
        return new Invitation(
                rs.getObject("id", UUID.class),
                new UserId(rs.getObject("user_id", UUID.class)),
                InvitationPurpose.valueOf(rs.getString("purpose")),
                rs.getString("token_hash"),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                usedAt == null ? null : usedAt.toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
