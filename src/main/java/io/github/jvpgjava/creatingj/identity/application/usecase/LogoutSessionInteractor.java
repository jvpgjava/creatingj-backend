package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.in.LogoutSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;

import java.time.Instant;
import java.util.UUID;

public final class LogoutSessionInteractor implements LogoutSessionUseCase {

    private final SessionStorePort sessionStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public LogoutSessionInteractor(SessionStorePort sessionStore, AuditPort auditPort, ClockPort clock) {
        this.sessionStore = sessionStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void execute(LogoutSessionCommand command) {
        Instant now = clock.now();
        sessionStore.findById(command.sessionId())
                .filter(session -> session.userId().equals(command.actorUserId()) && session.revokedAt() == null)
                .ifPresent(session -> {
                    sessionStore.save(session.revoke(now));
                    auditPort.record(AuditEvent.of(
                            UUID.randomUUID(),
                            command.actorUserId().value(),
                            command.actorUserId().value(),
                            AuditAction.LOGOUT,
                            "SUCCESS",
                            "Sessao encerrada pelo usuario.",
                            now
                    ));
                });
    }
}
