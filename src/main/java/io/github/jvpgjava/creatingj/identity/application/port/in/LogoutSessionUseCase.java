package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.UUID;

public interface LogoutSessionUseCase {

    void execute(LogoutSessionCommand command);

    record LogoutSessionCommand(UserId actorUserId, UUID sessionId) {
    }
}
