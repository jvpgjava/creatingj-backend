package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;
import java.util.Set;

public record WebLoginResponse(
        String userId,
        String email,
        Set<String> roles,
        Instant sessionExpiresAt
) {
}
