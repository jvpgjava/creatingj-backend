package io.github.jvpgjava.creatingj.device.application.port.out;

import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;
import java.util.Optional;

public interface DeviceStorePort {

    Device save(Device device);

    Optional<Device> findById(DeviceId deviceId);

    List<Device> findByOwner(UserId ownerUserId);
}
