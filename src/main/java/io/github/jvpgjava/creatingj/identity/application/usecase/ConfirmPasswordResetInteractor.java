package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ConfirmPasswordResetUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.PasswordPolicy;
import io.github.jvpgjava.creatingj.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

public final class ConfirmPasswordResetInteractor implements ConfirmPasswordResetUseCase {

    private final UserStorePort userStore;
    private final InvitationStorePort invitationStore;
    private final SessionStorePort sessionStore;
    private final PasswordHasherPort passwordHasher;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public ConfirmPasswordResetInteractor(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            SessionStorePort sessionStore,
            PasswordHasherPort passwordHasher,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.userStore = userStore;
        this.invitationStore = invitationStore;
        this.sessionStore = sessionStore;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void execute(ConfirmPasswordResetCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Token de redefinicao invalido.");
        }
        PasswordPolicy.validate(command.newRawPassword());

        String tokenHash = tokenGenerator.hash(command.token().trim());
        Invitation invitation = invitationStore.findByTokenHash(tokenHash)
                .filter(inv -> inv.purpose() == InvitationPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION_ERROR, "Token de redefinicao invalido."));

        Instant now = clock.now();
        Invitation usedInvitation = invitation.markUsed(now);

        User user = userStore.findById(invitation.userId())
                .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION_ERROR, "Token de redefinicao invalido."));

        String encodedPassword = passwordHasher.hash(command.newRawPassword());
        User updatedUser = user.changePassword(encodedPassword, now);

        invitationStore.save(usedInvitation);
        userStore.save(updatedUser);
        sessionStore.revokeAllByUserId(updatedUser.id(), now);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                updatedUser.id().value(),
                updatedUser.id().value(),
                AuditAction.PASSWORD_RESET_COMPLETED,
                "SUCCESS",
                "Senha redefinida e sessoes anteriores revogadas.",
                now
        ));
    }
}
