package io.github.jvpgjava.creatingj.device.adapter.out.persistence;

import io.github.jvpgjava.creatingj.device.application.port.out.PairingChallengeStorePort;
import io.github.jvpgjava.creatingj.device.domain.DevicePlatform;
import io.github.jvpgjava.creatingj.device.domain.PairingChallenge;
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
public class JdbcPairingChallengeStoreAdapter implements PairingChallengeStorePort {

    private final JdbcClient jdbcClient;

    public JdbcPairingChallengeStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PairingChallenge save(PairingChallenge challenge) {
        jdbcClient.sql("""
                INSERT INTO device_pairing_challenges (
                    id, owner_user_id, device_name, platform, code_hash, expires_at, consumed_at, created_at
                )
                VALUES (
                    :id, :ownerUserId, :deviceName, :platform, :codeHash, :expiresAt, :consumedAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    consumed_at = EXCLUDED.consumed_at
                """)
                .param("id", challenge.id())
                .param("ownerUserId", challenge.ownerUserId().value())
                .param("deviceName", challenge.deviceName())
                .param("platform", challenge.platform().name())
                .param("codeHash", challenge.codeHash())
                .param("expiresAt", OffsetDateTime.ofInstant(challenge.expiresAt(), ZoneOffset.UTC))
                .param("consumedAt", challenge.consumedAt() == null
                        ? null
                        : OffsetDateTime.ofInstant(challenge.consumedAt(), ZoneOffset.UTC))
                .param("createdAt", OffsetDateTime.ofInstant(challenge.createdAt(), ZoneOffset.UTC))
                .update();
        return challenge;
    }

    @Override
    public Optional<PairingChallenge> findByCodeHash(String codeHash) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, device_name, platform, code_hash, expires_at, consumed_at, created_at
                FROM device_pairing_challenges
                WHERE code_hash = :codeHash
                """)
                .param("codeHash", codeHash)
                .query(this::mapRow)
                .optional();
    }

    private PairingChallenge mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime consumedAt = rs.getObject("consumed_at", OffsetDateTime.class);
        return new PairingChallenge(
                rs.getObject("id", UUID.class),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("device_name"),
                DevicePlatform.valueOf(rs.getString("platform")),
                rs.getString("code_hash"),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                consumedAt == null ? (Instant) null : consumedAt.toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
