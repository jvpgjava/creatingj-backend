package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import java.util.List;

public record DevicesResponse(
        List<DeviceSummaryResponse> items
) {
}
