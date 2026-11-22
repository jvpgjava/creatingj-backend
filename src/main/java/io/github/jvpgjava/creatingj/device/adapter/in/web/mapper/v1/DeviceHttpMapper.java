package io.github.jvpgjava.creatingj.device.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CompletePairingResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DeviceSummaryResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DevicesResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.PairingChallengeResponse;
import io.github.jvpgjava.creatingj.device.application.port.in.CompleteDevicePairingUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.CreatePairingChallengeUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.ListUserDevicesUseCase;
import io.github.jvpgjava.creatingj.device.domain.Device;

import java.util.List;

public final class DeviceHttpMapper {

    public PairingChallengeResponse toChallenge(CreatePairingChallengeUseCase.Result result) {
        return new PairingChallengeResponse(
                result.challengeId().toString(),
                result.pairingCode(),
                result.deviceName(),
                result.platform().name(),
                result.expiresAt()
        );
    }

    public DeviceSummaryResponse toSummary(Device device, boolean online) {
        return new DeviceSummaryResponse(
                device.id().toString(),
                device.name(),
                device.platform().name(),
                device.status().name(),
                online,
                device.lastHeartbeatAt(),
                device.createdAt(),
                device.revokedAt()
        );
    }

    public CompletePairingResponse toCompletePairing(CompleteDevicePairingUseCase.Result result) {
        return new CompletePairingResponse(toSummary(result.device(), true), result.deviceToken());
    }

    public DevicesResponse toDevices(List<ListUserDevicesUseCase.DeviceView> views) {
        return new DevicesResponse(views.stream().map(v -> toSummary(v.device(), v.online())).toList());
    }
}
