package io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1;

import java.time.Instant;

public record ProjectResponse(
        String id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
