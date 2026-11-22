package io.github.jvpgjava.creatingj.device.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record Device(
        DeviceId id,
        UserId ownerUserId,
        String name,
        DevicePlatform platform,
        DeviceStatus status,
        Instant lastHeartbeatAt,
        Instant createdAt,
        Instant revokedAt
) {

    public static final Duration HEARTBEAT_TTL = Duration.ofSeconds(60);

    public Device {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Nome do dispositivo invalido.");
        }
        name = name.trim();
    }

    public boolean isOwnedBy(UserId candidate) {
        return ownerUserId.equals(candidate);
    }

    public boolean isActive() {
        return status == DeviceStatus.ACTIVE;
    }

    public boolean isOnline(Instant now) {
        return isActive()
                && lastHeartbeatAt != null
                && !lastHeartbeatAt.plus(HEARTBEAT_TTL).isBefore(now);
    }

    public Device recordHeartbeat(Instant now) {
        if (!isActive()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Dispositivo revogado.");
        }
        return new Device(id, ownerUserId, name, platform, status, now, createdAt, revokedAt);
    }

    public Device revoke(Instant now) {
        if (status == DeviceStatus.REVOKED) {
            return this;
        }
        return new Device(id, ownerUserId, name, platform, DeviceStatus.REVOKED, lastHeartbeatAt, createdAt, now);
    }
}
