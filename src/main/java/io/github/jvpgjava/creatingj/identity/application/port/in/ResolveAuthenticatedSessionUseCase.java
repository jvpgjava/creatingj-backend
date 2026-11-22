package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ResolveAuthenticatedSessionUseCase {

    Optional<AuthenticatedPrincipal> execute(String rawToken);

    record AuthenticatedPrincipal(
            UUID sessionId,
            UserId userId,
            String email,
            Set<Role> roles
    ) {
    }
}
