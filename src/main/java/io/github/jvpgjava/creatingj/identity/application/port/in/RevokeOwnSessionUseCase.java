package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.UUID;

public interface RevokeOwnSessionUseCase {

    void execute(RevokeOwnSessionCommand command);

    record RevokeOwnSessionCommand(UserId actorUserId, UUID sessionId) {
    }
}
