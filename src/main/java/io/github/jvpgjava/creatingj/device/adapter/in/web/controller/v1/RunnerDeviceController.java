package io.github.jvpgjava.creatingj.device.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.apidocs.v1.RunnerDeviceApiDocs;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CompletePairingRequest;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CompletePairingResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DeviceSummaryResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.mapper.v1.DeviceHttpMapper;
import io.github.jvpgjava.creatingj.device.adapter.in.web.security.AuthenticatedDevice;
import io.github.jvpgjava.creatingj.device.application.port.in.CompleteDevicePairingUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.RecordDeviceHeartbeatUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runner")
public final class RunnerDeviceController implements RunnerDeviceApiDocs {

    private final CompleteDevicePairingUseCase completePairing;
    private final RecordDeviceHeartbeatUseCase recordHeartbeat;
    private final DeviceHttpMapper mapper;

    public RunnerDeviceController(
            CompleteDevicePairingUseCase completePairing,
            RecordDeviceHeartbeatUseCase recordHeartbeat,
            DeviceHttpMapper mapper
    ) {
        this.completePairing = completePairing;
        this.recordHeartbeat = recordHeartbeat;
        this.mapper = mapper;
    }

    @Override
    @PostMapping("/pair/complete")
    public ResponseEntity<CompletePairingResponse> completePairing(
            @Valid @RequestBody CompletePairingRequest request
    ) {
        var result = completePairing.execute(new CompleteDevicePairingUseCase.Command(request.pairingCode()));
        return ResponseEntity.ok(mapper.toCompletePairing(result));
    }

    @Override
    @PostMapping("/heartbeat")
    public ResponseEntity<DeviceSummaryResponse> heartbeat(
            @AuthenticationPrincipal AuthenticatedDevice device
    ) {
        var updated = recordHeartbeat.execute(device.deviceId());
        return ResponseEntity.ok(mapper.toSummary(updated, true));
    }
}
