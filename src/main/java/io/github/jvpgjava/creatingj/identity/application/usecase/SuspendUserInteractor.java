package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.SuspendUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

public final class SuspendUserInteractor implements SuspendUserUseCase {

    private final UserStorePort userStore;
    private final SessionStorePort sessionStore;
    private final InvitationStorePort invitationStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public SuspendUserInteractor(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            InvitationStorePort invitationStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.invitationStore = invitationStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public User execute(SuspendUserCommand command) {
        User actor = AdminAccessGuard.requireActiveAdmin(userStore, command.actorUserId());
        if (actor.id().equals(command.targetUserId())) {
            throw new DomainException(ErrorCode.CONFLICT, "Administrador nao pode suspender a propria conta.");
        }
        User target = userStore.findById(command.targetUserId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Usuario nao encontrado."));

        Instant now = clock.now();
        User suspended = userStore.save(target.suspend(now));
        sessionStore.revokeAllByUserId(suspended.id(), now);
        invitationStore.invalidatePendingByUserAndPurpose(suspended.id(), InvitationPurpose.ACCOUNT_ACTIVATION, now);
        invitationStore.invalidatePendingByUserAndPurpose(suspended.id(), InvitationPurpose.PASSWORD_RESET, now);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                actor.id().value(),
                suspended.id().value(),
                AuditAction.USER_SUSPENDED,
                "SUCCESS",
                "Usuario suspenso e sessoes revogadas.",
                now
        ));

        return suspended;
    }
}
