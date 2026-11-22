package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.util.List;

public interface ListOwnSessionsUseCase {

    List<UserSession> execute(UserId actorUserId);
}
