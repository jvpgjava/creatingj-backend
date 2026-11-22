package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.GetCurrentUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Instant;
import java.util.UUID;

public final class GetCurrentUserInteractor implements GetCurrentUserUseCase {

    private final UserStorePort userStore;
    private final SessionStorePort sessionStore;
    private final ClockPort clock;

    public GetCurrentUserInteractor(UserStorePort userStore, SessionStorePort sessionStore, ClockPort clock) {
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.clock = clock;
    }

    @Override
    public CurrentUserProfile execute(UserId actorUserId, UUID currentSessionId) {
        User user = userStore.findById(actorUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Sessao invalida."));

        Instant now = clock.now();
        UserSession session = sessionStore.findById(currentSessionId)
                .filter(s -> s.userId().equals(actorUserId) && s.isActiveAt(now))
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Sessao invalida."));

        return new CurrentUserProfile(user, session);
    }
}
