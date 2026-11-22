package io.github.jvpgjava.creatingj.device.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.in.ListUserDevicesUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.List;

public final class ListUserDevicesInteractor implements ListUserDevicesUseCase {

    private final DeviceStorePort deviceStore;
    private final ClockPort clock;

    public ListUserDevicesInteractor(DeviceStorePort deviceStore, ClockPort clock) {
        this.deviceStore = deviceStore;
        this.clock = clock;
    }

    @Override
    public List<DeviceView> execute(UserId actorUserId) {
        if (actorUserId == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        Instant now = clock.now();
        return deviceStore.findByOwner(actorUserId).stream()
                .map(d -> new DeviceView(d, d.isOnline(now)))
                .toList();
    }
}
