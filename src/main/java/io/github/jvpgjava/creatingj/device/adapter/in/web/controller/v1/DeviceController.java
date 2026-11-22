package io.github.jvpgjava.creatingj.device.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.apidocs.v1.DeviceApiDocs;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CreatePairingChallengeRequest;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DeviceSummaryResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DevicesResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.PairingChallengeResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.mapper.v1.DeviceHttpMapper;
import io.github.jvpgjava.creatingj.device.application.port.in.CreatePairingChallengeUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.ListUserDevicesUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.RevokeDeviceUseCase;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public final class DeviceController implements DeviceApiDocs {

    private final CreatePairingChallengeUseCase createChallenge;
    private final ListUserDevicesUseCase listDevices;
    private final RevokeDeviceUseCase revokeDevice;
    private final DeviceHttpMapper mapper;

    public DeviceController(
            CreatePairingChallengeUseCase createChallenge,
            ListUserDevicesUseCase listDevices,
            RevokeDeviceUseCase revokeDevice,
            DeviceHttpMapper mapper
    ) {
        this.createChallenge = createChallenge;
        this.listDevices = listDevices;
        this.revokeDevice = revokeDevice;
        this.mapper = mapper;
    }

    @Override
    @PostMapping("/pairing-challenges")
    public ResponseEntity<PairingChallengeResponse> createPairingChallenge(
            @Valid @RequestBody CreatePairingChallengeRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = createChallenge.execute(new CreatePairingChallengeUseCase.Command(
                actor.id(),
                request.deviceName(),
                request.platform()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toChallenge(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<DevicesResponse> listDevices(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(mapper.toDevices(listDevices.execute(actor.id())));
    }

    @Override
    @PostMapping("/{deviceId}/revoke")
    public ResponseEntity<DeviceSummaryResponse> revokeDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var revoked = revokeDevice.execute(new RevokeDeviceUseCase.Command(actor.id(), DeviceId.parse(deviceId)));
        return ResponseEntity.ok(mapper.toSummary(revoked, false));
    }
}
