package io.github.jvpgjava.creatingj.device.adapter.out.persistence;

import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.domain.DeviceCredential;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
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
public class JdbcDeviceCredentialStoreAdapter implements DeviceCredentialStorePort {

    private final JdbcClient jdbcClient;

    public JdbcDeviceCredentialStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public DeviceCredential save(DeviceCredential credential) {
        jdbcClient.sql("""
                INSERT INTO device_credentials (id, device_id, owner_user_id, token_hash, created_at, revoked_at)
                VALUES (:id, :deviceId, :ownerUserId, :tokenHash, :createdAt, :revokedAt)
                ON CONFLICT (id) DO UPDATE SET
                    revoked_at = EXCLUDED.revoked_at
                """)
                .param("id", credential.id())
                .param("deviceId", credential.deviceId().value())
                .param("ownerUserId", credential.ownerUserId().value())
                .param("tokenHash", credential.tokenHash())
                .param("createdAt", OffsetDateTime.ofInstant(credential.createdAt(), ZoneOffset.UTC))
                .param("revokedAt", credential.revokedAt() == null
                        ? null
                        : OffsetDateTime.ofInstant(credential.revokedAt(), ZoneOffset.UTC))
                .update();
        return credential;
    }

    @Override
    public Optional<DeviceCredential> findByTokenHash(String tokenHash) {
        return jdbcClient.sql("""
                SELECT id, device_id, owner_user_id, token_hash, created_at, revoked_at
                FROM device_credentials
                WHERE token_hash = :tokenHash
                """)
                .param("tokenHash", tokenHash)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public void revokeAllByDeviceId(DeviceId deviceId, Instant revokedAt) {
        jdbcClient.sql("""
                UPDATE device_credentials
                SET revoked_at = :revokedAt
                WHERE device_id = :deviceId AND revoked_at IS NULL
                """)
                .param("revokedAt", OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC))
                .param("deviceId", deviceId.value())
                .update();
    }

    private DeviceCredential mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime revokedAt = rs.getObject("revoked_at", OffsetDateTime.class);
        return new DeviceCredential(
                rs.getObject("id", UUID.class),
                new DeviceId(rs.getObject("device_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("token_hash"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                revokedAt == null ? (Instant) null : revokedAt.toInstant()
        );
    }
}
