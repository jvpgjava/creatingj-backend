package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;

public record PkceAuthorizeResponse(
        String authorizationCode,
        String redirectUri,
        String state,
        Instant expiresAt
) {
}
