package io.github.jvpgjava.creatingj.device.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PairingChallenge(
        UUID id,
        UserId ownerUserId,
        String deviceName,
        DevicePlatform platform,
        String codeHash,
        Instant expiresAt,
        Instant consumedAt,
        Instant createdAt
) {

    public PairingChallenge {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(codeHash, "codeHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");
        if (deviceName == null || deviceName.isBlank() || deviceName.trim().length() > 120) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Nome do dispositivo invalido.");
        }
        deviceName = deviceName.trim();
    }

    public boolean isConsumableAt(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public PairingChallenge consume(Instant now) {
        if (!isConsumableAt(now)) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Codigo de pareamento invalido ou expirado.");
        }
        return new PairingChallenge(id, ownerUserId, deviceName, platform, codeHash, expiresAt, now, createdAt);
    }
}
