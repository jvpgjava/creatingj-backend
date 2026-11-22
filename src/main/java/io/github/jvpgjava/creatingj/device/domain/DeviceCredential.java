package io.github.jvpgjava.creatingj.device.domain;

import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DeviceCredential(
        UUID id,
        DeviceId deviceId,
        UserId ownerUserId,
        String tokenHash,
        Instant createdAt,
        Instant revokedAt
) {

    public DeviceCredential {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public DeviceCredential revoke(Instant now) {
        return revokedAt != null ? this : new DeviceCredential(id, deviceId, ownerUserId, tokenHash, createdAt, now);
    }
}
