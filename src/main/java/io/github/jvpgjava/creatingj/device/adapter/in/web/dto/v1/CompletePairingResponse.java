package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

public record CompletePairingResponse(
        DeviceSummaryResponse device,
        String deviceToken
) {
}
