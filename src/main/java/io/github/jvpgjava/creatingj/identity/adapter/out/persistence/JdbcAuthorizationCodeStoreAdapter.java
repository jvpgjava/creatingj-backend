package io.github.jvpgjava.creatingj.identity.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.PkceChallenge;
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
public class JdbcAuthorizationCodeStoreAdapter implements AuthorizationCodeStorePort {

    private final JdbcClient jdbcClient;

    public JdbcAuthorizationCodeStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public AuthorizationCode save(AuthorizationCode code) {
        jdbcClient.sql("""
                        INSERT INTO authorization_codes
                            (id, user_id, client_id, redirect_uri, code_hash,
                             code_challenge, code_challenge_method, expires_at, used_at, created_at)
                        VALUES
                            (:id, :userId, :clientId, :redirectUri, :codeHash,
                             :codeChallenge, :codeChallengeMethod, :expiresAt, :usedAt, :createdAt)
                        ON CONFLICT (id) DO UPDATE SET
                            used_at = EXCLUDED.used_at
                        """)
                .param("id", code.id())
                .param("userId", code.userId().value())
                .param("clientId", code.clientId())
                .param("redirectUri", code.redirectUri())
                .param("codeHash", code.codeHash())
                .param("codeChallenge", code.challenge().value())
                .param("codeChallengeMethod", code.challenge().method())
                .param("expiresAt", toOffset(code.expiresAt()))
                .param("usedAt", toOffset(code.usedAt()))
                .param("createdAt", toOffset(code.createdAt()))
                .update();
        return code;
    }

    @Override
    public Optional<AuthorizationCode> findByCodeHash(String codeHash) {
        return jdbcClient.sql("""
                        SELECT id, user_id, client_id, redirect_uri, code_hash,
                               code_challenge, code_challenge_method, expires_at, used_at, created_at
                        FROM authorization_codes
                        WHERE code_hash = :codeHash
                        """)
                .param("codeHash", codeHash)
                .query(this::mapRow)
                .optional();
    }

    private AuthorizationCode mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime usedAt = rs.getObject("used_at", OffsetDateTime.class);
        return new AuthorizationCode(
                rs.getObject("id", UUID.class),
                new UserId(rs.getObject("user_id", UUID.class)),
                rs.getString("client_id"),
                rs.getString("redirect_uri"),
                rs.getString("code_hash"),
                PkceChallenge.s256(rs.getString("code_challenge"), rs.getString("code_challenge_method")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                usedAt == null ? null : usedAt.toInstant()
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
