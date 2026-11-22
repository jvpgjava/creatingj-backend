package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.in.RequestPasswordResetUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class RequestPasswordResetInteractor implements RequestPasswordResetUseCase {

    private final UserStorePort userStore;
    private final InvitationStorePort invitationStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final Duration resetTtl;

    public RequestPasswordResetInteractor(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock,
            Duration resetTtl
    ) {
        this.userStore = userStore;
        this.invitationStore = invitationStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.resetTtl = resetTtl;
    }

    @Override
    public PasswordResetRequestResult execute(RequestPasswordResetCommand command) {
        Email email = Email.of(command.email());
        Optional<User> optionalUser = userStore.findByEmail(email);
        if (optionalUser.isEmpty() || !optionalUser.get().isActive()) {
            return new PasswordResetRequestResult(Optional.empty());
        }

        User user = optionalUser.get();
        Instant now = clock.now();
        invitationStore.invalidatePendingByUserAndPurpose(user.id(), InvitationPurpose.PASSWORD_RESET, now);

        String rawToken = tokenGenerator.generateToken();
        Invitation resetInvitation = Invitation.create(
                UUID.randomUUID(),
                user.id(),
                InvitationPurpose.PASSWORD_RESET,
                tokenGenerator.hash(rawToken),
                now,
                now.plus(resetTtl)
        );
        invitationStore.save(resetInvitation);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                user.id().value(),
                user.id().value(),
                AuditAction.PASSWORD_RESET_REQUESTED,
                "SUCCESS",
                "Solicitacao de redefinicao de senha registrada.",
                now
        ));

        return new PasswordResetRequestResult(Optional.of(rawToken));
    }
}
