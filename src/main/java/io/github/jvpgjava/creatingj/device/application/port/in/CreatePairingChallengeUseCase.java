package io.github.jvpgjava.creatingj.device.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.DevicePlatform;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.UUID;

public interface CreatePairingChallengeUseCase {

    Result execute(Command command);

    record Command(UserId actorUserId, String deviceName, DevicePlatform platform) {
    }

    record Result(UUID challengeId, String pairingCode, String deviceName, DevicePlatform platform, Instant expiresAt) {
    }
}
