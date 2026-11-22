package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.RevokeOwnSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Instant;
import java.util.UUID;

public final class RevokeOwnSessionInteractor implements RevokeOwnSessionUseCase {

    private final SessionStorePort sessionStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public RevokeOwnSessionInteractor(SessionStorePort sessionStore, AuditPort auditPort, ClockPort clock) {
        this.sessionStore = sessionStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void execute(RevokeOwnSessionCommand command) {
        UserSession session = sessionStore.findById(command.sessionId())
                .filter(s -> s.userId().equals(command.actorUserId()))
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Sessao nao encontrada."));

        Instant now = clock.now();
        if (session.revokedAt() == null) {
            sessionStore.save(session.revoke(now));
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    command.actorUserId().value(),
                    command.actorUserId().value(),
                    AuditAction.SESSION_REVOKED,
                    "SUCCESS",
                    "Sessao revogada pelo proprio usuario.",
                    now
            ));
        }
    }
}
