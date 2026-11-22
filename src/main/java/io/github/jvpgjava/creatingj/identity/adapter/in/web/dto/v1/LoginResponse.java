package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;
import java.util.Set;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String userId,
        String email,
        Set<String> roles
) {
}
