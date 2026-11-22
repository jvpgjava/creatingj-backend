package io.github.jvpgjava.creatingj.device.domain;

import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.UUID;

public record AuthenticatedDevicePrincipal(
        DeviceId deviceId,
        UserId ownerUserId,
        UUID credentialId
) {
}
