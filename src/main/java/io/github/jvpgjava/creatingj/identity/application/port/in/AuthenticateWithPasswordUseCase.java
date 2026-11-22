package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface AuthenticateWithPasswordUseCase {

    AuthenticatedSession execute(LoginCommand command);

    record LoginCommand(
            String email,
            String rawPassword,
            String clientId,
            String authMethod
    ) {
        public LoginCommand(String email, String rawPassword) {
            this(email, rawPassword, "api", "PASSWORD");
        }
    }

    record AuthenticatedSession(
            UUID sessionId,
            String accessToken,
            Instant expiresAt,
            UserId userId,
            String email,
            Set<Role> roles
    ) {
    }
}
