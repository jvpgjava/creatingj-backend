package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;
import java.util.Set;

public record TokenPairResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String userId,
        String email,
        Set<String> roles
) {
}
