package io.github.jvpgjava.creatingj.device.adapter.in.web.security;

import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.UUID;

public record AuthenticatedDevice(
        DeviceId deviceId,
        UserId ownerUserId,
        UUID credentialId
) {
}
