package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.PublicClientRegistry;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class ExchangePkceCodeInteractor implements ExchangePkceCodeUseCase {

    private final UserStorePort userStore;
    private final AuthorizationCodeStorePort authorizationCodeStore;
    private final SessionStorePort sessionStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public ExchangePkceCodeInteractor(
            UserStorePort userStore,
            AuthorizationCodeStorePort authorizationCodeStore,
            SessionStorePort sessionStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {
        this.userStore = userStore;
        this.authorizationCodeStore = authorizationCodeStore;
        this.sessionStore = sessionStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    public TokenPairResult execute(ExchangePkceCodeCommand command) {
        if (command.code() == null || command.code().isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Codigo de autorizacao obrigatorio.");
        }
        PublicClientRegistry.validate(command.clientId(), command.redirectUri());

        AuthorizationCode code = authorizationCodeStore.findByCodeHash(tokenGenerator.hash(command.code().trim()))
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Codigo de autorizacao invalido."));

        if (!code.matchesClientAndRedirect(command.clientId().trim(), command.redirectUri().trim())
                || !code.challenge().matchesVerifier(command.codeVerifier())) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Validacao PKCE falhou.");
        }

        Instant now = clock.now();
        AuthorizationCode usedCode = code.markUsed(now);

        User user = userStore.findById(code.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Conta indisponivel."));

        authorizationCodeStore.save(usedCode);

        String rawAccess = tokenGenerator.generateToken();
        String rawRefresh = tokenGenerator.generateToken();
        Instant accessExp = now.plus(accessTokenTtl);
        Instant refreshExp = now.plus(refreshTokenTtl);
        UUID sessionId = UUID.randomUUID();
        UserSession session = UserSession.createWithRefresh(
                sessionId,
                user.id(),
                code.clientId(),
                "PKCE",
                sessionId,
                tokenGenerator.hash(rawAccess),
                now,
                accessExp,
                tokenGenerator.hash(rawRefresh),
                refreshExp
        );
        sessionStore.save(session);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                user.id().value(),
                user.id().value(),
                AuditAction.PKCE_TOKEN_EXCHANGED,
                "SUCCESS",
                "Sessao PKCE emitida para cliente publico.",
                now
        ));

        return new TokenPairResult(
                session.id(),
                rawAccess,
                accessExp,
                rawRefresh,
                refreshExp,
                user.id(),
                user.email().value(),
                user.roles()
        );
    }
}
