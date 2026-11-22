package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.LoginAttemptGuardPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class AuthenticateWithPasswordInteractor implements AuthenticateWithPasswordUseCase {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciais invalidas.";

    private final UserStorePort userStore;
    private final SessionStorePort sessionStore;
    private final PasswordHasherPort passwordHasher;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final LoginAttemptGuardPort loginAttemptGuard;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final Duration sessionTtl;

    public AuthenticateWithPasswordInteractor(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            PasswordHasherPort passwordHasher,
            OpaqueTokenGeneratorPort tokenGenerator,
            LoginAttemptGuardPort loginAttemptGuard,
            AuditPort auditPort,
            ClockPort clock,
            Duration sessionTtl
    ) {
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.loginAttemptGuard = loginAttemptGuard;
        this.auditPort = auditPort;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    @Override
    public AuthenticatedSession execute(LoginCommand command) {
        Email email = Email.of(command.email());
        String attemptKey = email.value();
        Instant now = clock.now();

        if (loginAttemptGuard.isBlocked(attemptKey)) {
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    null,
                    null,
                    AuditAction.LOGIN_BLOCKED,
                    "BLOCKED",
                    "Tentativa de login bloqueada por limite de tentativas.",
                    now
            ));
            throw new DomainException(ErrorCode.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Tente novamente mais tarde.");
        }

        Optional<User> optionalUser = userStore.findByEmail(email);
        if (optionalUser.isEmpty() || !optionalUser.get().isActive()
                || command.rawPassword() == null
                || !passwordHasher.matches(command.rawPassword(), optionalUser.get().passwordHash())) {
            loginAttemptGuard.recordFailure(attemptKey);
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    null,
                    optionalUser.map(u -> u.id().value()).orElse(null),
                    AuditAction.LOGIN_FAILED,
                    "FAILURE",
                    "Falha de autenticacao por credenciais ou status da conta.",
                    now
            ));
            throw new DomainException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        User user = optionalUser.get();
        loginAttemptGuard.reset(attemptKey);

        String rawToken = tokenGenerator.generateToken();
        Instant expiresAt = now.plus(sessionTtl);
        String clientId = (command.clientId() == null || command.clientId().isBlank()) ? "api" : command.clientId();
        String authMethod = (command.authMethod() == null || command.authMethod().isBlank())
                ? "PASSWORD" : command.authMethod();
        UserSession session = UserSession.createForClient(
                UUID.randomUUID(),
                user.id(),
                clientId,
                authMethod,
                tokenGenerator.hash(rawToken),
                now,
                expiresAt
        );
        sessionStore.save(session);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                user.id().value(),
                user.id().value(),
                AuditAction.LOGIN_SUCCEEDED,
                "SUCCESS",
                "Sessao autenticada criada.",
                now
        ));

        return new AuthenticatedSession(session.id(), rawToken, expiresAt, user.id(), user.email().value(),
                user.roles());
    }
}
