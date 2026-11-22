package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListOwnSessionsUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.util.List;

public final class ListOwnSessionsInteractor implements ListOwnSessionsUseCase {

    private final SessionStorePort sessionStore;
    private final ClockPort clock;

    public ListOwnSessionsInteractor(SessionStorePort sessionStore, ClockPort clock) {
        this.sessionStore = sessionStore;
        this.clock = clock;
    }

    @Override
    public List<UserSession> execute(UserId actorUserId) {
        return sessionStore.findActiveByUserId(actorUserId, clock.now());
    }
}
