package io.github.jvpgjava.creatingj.identity.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSessionStoreAdapter implements SessionStorePort {

    private static final String BASE_SELECT = """
            SELECT id, user_id, client_id, auth_method, family_id, token_hash,
                   created_at, expires_at, revoked_at,
                   refresh_token_hash, refresh_expires_at, rotated_at
            FROM sessions
            """;

    private final JdbcClient jdbcClient;

    public JdbcSessionStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UserSession save(UserSession session) {
        jdbcClient.sql("""
                        INSERT INTO sessions
                            (id, user_id, client_id, auth_method, family_id, token_hash,
                             created_at, expires_at, revoked_at,
                             refresh_token_hash, refresh_expires_at, rotated_at)
                        VALUES
                            (:id, :userId, :clientId, :authMethod, :familyId, :tokenHash,
                             :createdAt, :expiresAt, :revokedAt,
                             :refreshTokenHash, :refreshExpiresAt, :rotatedAt)
                        ON CONFLICT (id) DO UPDATE SET
                            revoked_at = EXCLUDED.revoked_at,
                            rotated_at = EXCLUDED.rotated_at
                        """)
                .param("id", session.id())
                .param("userId", session.userId().value())
                .param("clientId", session.clientId())
                .param("authMethod", session.authMethod())
                .param("familyId", session.familyId())
                .param("tokenHash", session.tokenHash())
                .param("createdAt", toOffset(session.createdAt()))
                .param("expiresAt", toOffset(session.expiresAt()))
                .param("revokedAt", toOffset(session.revokedAt()))
                .param("refreshTokenHash", session.refreshTokenHash())
                .param("refreshExpiresAt", toOffset(session.refreshExpiresAt()))
                .param("rotatedAt", toOffset(session.rotatedAt()))
                .update();
        return session;
    }

    @Override
    public Optional<UserSession> findById(UUID sessionId) {
        return jdbcClient.sql(BASE_SELECT + " WHERE id = :id")
                .param("id", sessionId)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<UserSession> findByTokenHash(String tokenHash) {
        return jdbcClient.sql(BASE_SELECT + " WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
        return jdbcClient.sql(BASE_SELECT + " WHERE refresh_token_hash = :refreshTokenHash")
                .param("refreshTokenHash", refreshTokenHash)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<UserSession> findActiveByUserId(UserId userId, Instant now) {
        return jdbcClient.sql(BASE_SELECT + """
                         WHERE user_id = :userId
                           AND revoked_at IS NULL
                           AND rotated_at IS NULL
                           AND expires_at > :now
                         ORDER BY created_at DESC
                        """)
                .param("userId", userId.value())
                .param("now", toOffset(now))
                .query(this::mapRow)
                .list();
    }

    @Override
    public void revokeAllByUserId(UserId userId, Instant revokedAt) {
        jdbcClient.sql("""
                        UPDATE sessions
                        SET revoked_at = :revokedAt
                        WHERE user_id = :userId
                          AND revoked_at IS NULL
                        """)
                .param("revokedAt", toOffset(revokedAt))
                .param("userId", userId.value())
                .update();
    }

    @Override
    public void revokeByFamilyId(UUID familyId, Instant revokedAt) {
        jdbcClient.sql("""
                        UPDATE sessions
                        SET revoked_at = :revokedAt
                        WHERE family_id = :familyId
                          AND revoked_at IS NULL
                        """)
                .param("revokedAt", toOffset(revokedAt))
                .param("familyId", familyId)
                .update();
    }

    private UserSession mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserSession(
                rs.getObject("id", UUID.class),
                new UserId(rs.getObject("user_id", UUID.class)),
                rs.getString("client_id"),
                rs.getString("auth_method"),
                rs.getObject("family_id", UUID.class),
                rs.getString("token_hash"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("expires_at", OffsetDateTime.class)),
                toInstant(rs.getObject("revoked_at", OffsetDateTime.class)),
                rs.getString("refresh_token_hash"),
                toInstant(rs.getObject("refresh_expires_at", OffsetDateTime.class)),
                toInstant(rs.getObject("rotated_at", OffsetDateTime.class))
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
