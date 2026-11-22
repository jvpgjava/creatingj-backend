package io.github.jvpgjava.creatingj.device.application.port.out;

import io.github.jvpgjava.creatingj.device.domain.DeviceCredential;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;

import java.time.Instant;
import java.util.Optional;

public interface DeviceCredentialStorePort {

    DeviceCredential save(DeviceCredential credential);

    Optional<DeviceCredential> findByTokenHash(String tokenHash);

    void revokeAllByDeviceId(DeviceId deviceId, Instant revokedAt);
}
