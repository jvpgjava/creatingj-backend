package io.github.jvpgjava.creatingj.identity.adapter.in.web.security;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID sessionId,
        UserId id,
        String email,
        Set<Role> roles
) {
}
