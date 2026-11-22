package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.util.UUID;

public interface GetCurrentUserUseCase {

    CurrentUserProfile execute(UserId actorUserId, UUID currentSessionId);

    record CurrentUserProfile(User user, UserSession session) {
    }
}
