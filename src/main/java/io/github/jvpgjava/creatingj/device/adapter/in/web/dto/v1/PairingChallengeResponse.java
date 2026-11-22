package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import java.time.Instant;

public record PairingChallengeResponse(
        String challengeId,
        String pairingCode,
        String deviceName,
        String platform,
        Instant expiresAt
) {
}
