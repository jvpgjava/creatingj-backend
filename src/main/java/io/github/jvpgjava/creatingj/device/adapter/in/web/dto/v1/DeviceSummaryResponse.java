package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import java.time.Instant;

public record DeviceSummaryResponse(
        String id,
        String name,
        String platform,
        String status,
        boolean online,
        Instant lastHeartbeatAt,
        Instant createdAt,
        Instant revokedAt
) {
}
