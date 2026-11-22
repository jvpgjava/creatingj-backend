package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

public interface SuspendUserUseCase {

    User execute(SuspendUserCommand command);

    record SuspendUserCommand(
            UserId actorUserId,
            UserId targetUserId
    ) {
    }
}
