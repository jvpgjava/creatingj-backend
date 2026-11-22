package io.github.jvpgjava.creatingj.common.adapter.in.web.dto.v1;

import java.time.Instant;

public record SystemStatusResponse(
        String service,
        String apiVersion,
        String status,
        Instant timestamp
) {
}
