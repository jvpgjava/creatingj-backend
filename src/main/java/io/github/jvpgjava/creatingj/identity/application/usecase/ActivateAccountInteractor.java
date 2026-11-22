package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ActivateAccountUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.PasswordPolicy;
import io.github.jvpgjava.creatingj.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

public final class ActivateAccountInteractor implements ActivateAccountUseCase {

    private final UserStorePort userStore;
    private final InvitationStorePort invitationStore;
    private final PasswordHasherPort passwordHasher;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public ActivateAccountInteractor(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            PasswordHasherPort passwordHasher,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.userStore = userStore;
        this.invitationStore = invitationStore;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void execute(ActivateAccountCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Token de ativacao invalido.");
        }
        PasswordPolicy.validate(command.rawPassword());

        String tokenHash = tokenGenerator.hash(command.token().trim());
        Invitation invitation = invitationStore.findByTokenHash(tokenHash)
                .filter(inv -> inv.purpose() == InvitationPurpose.ACCOUNT_ACTIVATION)
                .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION_ERROR, "Token de ativacao invalido."));

        Instant now = clock.now();
        Invitation usedInvitation = invitation.markUsed(now);

        User user = userStore.findById(invitation.userId())
                .orElseThrow(() -> new DomainException(ErrorCode.VALIDATION_ERROR, "Token de ativacao invalido."));

        String encodedPassword = passwordHasher.hash(command.rawPassword());
        User activated = user.activate(encodedPassword, now);

        invitationStore.save(usedInvitation);
        userStore.save(activated);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                activated.id().value(),
                activated.id().value(),
                AuditAction.USER_ACTIVATED,
                "SUCCESS",
                "Conta ativada com token de uso unico.",
                now
        ));
    }
}
