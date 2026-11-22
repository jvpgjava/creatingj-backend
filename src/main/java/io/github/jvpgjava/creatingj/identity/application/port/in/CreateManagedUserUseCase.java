package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Set;

public interface CreateManagedUserUseCase {

    CreatedManagedUser execute(CreateManagedUserCommand command);

    record CreateManagedUserCommand(
            UserId actorUserId,
            String email,
            String displayName,
            Set<Role> roles
    ) {
    }

    record CreatedManagedUser(
            User user,
            String activationToken,
            Instant activationExpiresAt
    ) {
    }
}
