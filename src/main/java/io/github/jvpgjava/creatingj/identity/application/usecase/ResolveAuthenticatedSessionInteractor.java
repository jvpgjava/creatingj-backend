package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.in.ResolveAuthenticatedSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Instant;
import java.util.Optional;

public final class ResolveAuthenticatedSessionInteractor implements ResolveAuthenticatedSessionUseCase {

    private final SessionStorePort sessionStore;
    private final UserStorePort userStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final ClockPort clock;

    public ResolveAuthenticatedSessionInteractor(
            SessionStorePort sessionStore,
            UserStorePort userStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            ClockPort clock
    ) {
        this.sessionStore = sessionStore;
        this.userStore = userStore;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    @Override
    public Optional<AuthenticatedPrincipal> execute(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = tokenGenerator.hash(rawToken.trim());
        Optional<UserSession> sessionOpt = sessionStore.findByTokenHash(tokenHash);
        Instant now = clock.now();
        if (sessionOpt.isEmpty() || !sessionOpt.get().isActiveAt(now)) {
            return Optional.empty();
        }
        Optional<User> userOpt = userStore.findById(sessionOpt.get().userId());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        return Optional.of(new AuthenticatedPrincipal(
                sessionOpt.get().id(),
                user.id(),
                user.email().value(),
                user.roles()
        ));
    }
}
