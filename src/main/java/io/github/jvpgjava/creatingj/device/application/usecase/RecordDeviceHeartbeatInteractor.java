package io.github.jvpgjava.creatingj.device.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.in.RecordDeviceHeartbeatUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;

public final class RecordDeviceHeartbeatInteractor implements RecordDeviceHeartbeatUseCase {

    private final DeviceStorePort deviceStore;
    private final ClockPort clock;

    public RecordDeviceHeartbeatInteractor(DeviceStorePort deviceStore, ClockPort clock) {
        this.deviceStore = deviceStore;
        this.clock = clock;
    }

    @Override
    public Device execute(DeviceId deviceId) {
        Device device = deviceStore.findById(deviceId)
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Dispositivo nao autorizado."));
        return deviceStore.save(device.recordHeartbeat(clock.now()));
    }
}
