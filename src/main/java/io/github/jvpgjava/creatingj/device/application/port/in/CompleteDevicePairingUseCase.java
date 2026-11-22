package io.github.jvpgjava.creatingj.device.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.Device;

public interface CompleteDevicePairingUseCase {

    Result execute(Command command);

    record Command(String pairingCode) {
    }

    record Result(Device device, String deviceToken) {
    }
}
