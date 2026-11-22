package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.time.Instant;

public record CreatedManagedUserResponse(
        UserSummaryResponse user,
        String activationToken,
        Instant activationExpiresAt
) {
}
