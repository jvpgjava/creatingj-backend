package io.github.jvpgjava.creatingj.device.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;

public interface ListUserDevicesUseCase {

    List<DeviceView> execute(UserId actorUserId);

    record DeviceView(Device device, boolean online) {
    }
}
