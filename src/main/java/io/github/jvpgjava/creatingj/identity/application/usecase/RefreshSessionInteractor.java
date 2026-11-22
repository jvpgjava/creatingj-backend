package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase.TokenPairResult;
import io.github.jvpgjava.creatingj.identity.application.port.in.RefreshSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class RefreshSessionInteractor implements RefreshSessionUseCase {

    private final UserStorePort userStore;
    private final SessionStorePort sessionStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public RefreshSessionInteractor(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    public TokenPairResult execute(RefreshSessionCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Refresh token invalido.");
        }
        Instant now = clock.now();
        String refreshHash = tokenGenerator.hash(command.refreshToken().trim());
        UserSession existing = sessionStore.findByRefreshTokenHash(refreshHash)
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Refresh token invalido."));

        if (existing.isRotated()) {
            sessionStore.revokeByFamilyId(existing.familyId(), now);
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    existing.userId().value(),
                    existing.userId().value(),
                    AuditAction.REFRESH_REUSE_DETECTED,
                    "REVOKED",
                    "Reuso de refresh token detectado; familia de sessoes revogada.",
                    now
            ));
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Refresh token invalido.");
        }

        if (!existing.isRefreshableAt(now)) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Refresh token expirado ou revogado.");
        }

        User user = userStore.findById(existing.userId())
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Conta indisponivel."));

        sessionStore.save(existing.markRotated(now));

        String nextAccess = tokenGenerator.generateToken();
        String nextRefresh = tokenGenerator.generateToken();
        Instant accessExp = now.plus(accessTokenTtl);
        Instant refreshExp = now.plus(refreshTokenTtl);

        UserSession rotatedSession = UserSession.createWithRefresh(
                UUID.randomUUID(),
                user.id(),
                existing.clientId(),
                existing.authMethod(),
                existing.familyId(),
                tokenGenerator.hash(nextAccess),
                now,
                accessExp,
                tokenGenerator.hash(nextRefresh),
                refreshExp
        );
        sessionStore.save(rotatedSession);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                user.id().value(),
                user.id().value(),
                AuditAction.TOKEN_REFRESHED,
                "SUCCESS",
                "Sessao rotacionada via refresh token.",
                now
        ));

        return new TokenPairResult(
                rotatedSession.id(),
                nextAccess,
                accessExp,
                nextRefresh,
                refreshExp,
                user.id(),
                user.email().value(),
                user.roles()
        );
    }
}
