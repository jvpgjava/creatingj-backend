package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthorizePkceUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.PkceChallenge;
import io.github.jvpgjava.creatingj.identity.domain.PublicClientRegistry;
import io.github.jvpgjava.creatingj.identity.domain.User;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class AuthorizePkceInteractor implements AuthorizePkceUseCase {

    private final UserStorePort userStore;
    private final AuthorizationCodeStorePort authorizationCodeStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final Duration codeTtl;

    public AuthorizePkceInteractor(
            UserStorePort userStore,
            AuthorizationCodeStorePort authorizationCodeStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock,
            Duration codeTtl
    ) {
        this.userStore = userStore;
        this.authorizationCodeStore = authorizationCodeStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.codeTtl = codeTtl;
    }

    @Override
    public PkceAuthorizationResult execute(AuthorizePkceCommand command) {
        User actor = userStore.findById(command.actorUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Usuario autenticado indisponivel."));

        PublicClientRegistry.validate(command.clientId(), command.redirectUri());
        PkceChallenge challenge = PkceChallenge.s256(command.codeChallenge(), command.codeChallengeMethod());

        Instant now = clock.now();
        Instant expiresAt = now.plus(codeTtl);
        String rawCode = tokenGenerator.generateToken();
        AuthorizationCode authCode = AuthorizationCode.create(
                UUID.randomUUID(),
                actor.id(),
                command.clientId().trim(),
                command.redirectUri().trim(),
                tokenGenerator.hash(rawCode),
                challenge,
                now,
                expiresAt
        );
        authorizationCodeStore.save(authCode);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                actor.id().value(),
                actor.id().value(),
                AuditAction.PKCE_AUTHORIZED,
                "SUCCESS",
                "Codigo de autorizacao PKCE emitido para cliente publico.",
                now
        ));

        return new PkceAuthorizationResult(rawCode, authCode.redirectUri(), command.state(), expiresAt);
    }
}
