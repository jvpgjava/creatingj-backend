package io.github.jvpgjava.creatingj.device.adapter.out.persistence;

import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.device.domain.DevicePlatform;
import io.github.jvpgjava.creatingj.device.domain.DeviceStatus;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
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
public class JdbcDeviceStoreAdapter implements DeviceStorePort {

    private final JdbcClient jdbcClient;

    public JdbcDeviceStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Device save(Device device) {
        jdbcClient.sql("""
                INSERT INTO devices (
                    id, owner_user_id, name, platform, status, last_heartbeat_at, created_at, revoked_at
                )
                VALUES (
                    :id, :ownerUserId, :name, :platform, :status, :lastHeartbeatAt, :createdAt, :revokedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    last_heartbeat_at = EXCLUDED.last_heartbeat_at,
                    revoked_at = EXCLUDED.revoked_at
                """)
                .param("id", device.id().value())
                .param("ownerUserId", device.ownerUserId().value())
                .param("name", device.name())
                .param("platform", device.platform().name())
                .param("status", device.status().name())
                .param("lastHeartbeatAt", toOffset(device.lastHeartbeatAt()))
                .param("createdAt", toOffset(device.createdAt()))
                .param("revokedAt", toOffset(device.revokedAt()))
                .update();
        return device;
    }

    @Override
    public Optional<Device> findById(DeviceId deviceId) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, name, platform, status, last_heartbeat_at, created_at, revoked_at
                FROM devices
                WHERE id = :id
                """)
                .param("id", deviceId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Device> findByOwner(UserId ownerUserId) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, name, platform, status, last_heartbeat_at, created_at, revoked_at
                FROM devices
                WHERE owner_user_id = :ownerUserId
                ORDER BY created_at DESC
                """)
                .param("ownerUserId", ownerUserId.value())
                .query(this::mapRow)
                .list();
    }

    private Device mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Device(
                new DeviceId(rs.getObject("id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("name"),
                DevicePlatform.valueOf(rs.getString("platform")),
                DeviceStatus.valueOf(rs.getString("status")),
                toInstant(rs.getObject("last_heartbeat_at", OffsetDateTime.class)),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                toInstant(rs.getObject("revoked_at", OffsetDateTime.class))
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
