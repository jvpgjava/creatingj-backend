package io.github.jvpgjava.creatingj.device.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

public interface RevokeDeviceUseCase {

    Device execute(Command command);

    record Command(UserId actorUserId, DeviceId deviceId) {
    }
}
