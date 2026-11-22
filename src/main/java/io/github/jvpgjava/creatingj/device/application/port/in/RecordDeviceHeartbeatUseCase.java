package io.github.jvpgjava.creatingj.device.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;

public interface RecordDeviceHeartbeatUseCase {

    Device execute(DeviceId deviceId);
}
