package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.CreateManagedUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class CreateManagedUserInteractor implements CreateManagedUserUseCase {

    private final UserStorePort userStore;
    private final InvitationStorePort invitationStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final Duration activationTtl;

    public CreateManagedUserInteractor(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock,
            Duration activationTtl
    ) {
        this.userStore = userStore;
        this.invitationStore = invitationStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.activationTtl = activationTtl;
    }

    @Override
    public CreatedManagedUser execute(CreateManagedUserCommand command) {
        User actor = AdminAccessGuard.requireActiveAdmin(userStore, command.actorUserId());
        Email email = Email.of(command.email());
        if (userStore.existsByEmail(email)) {
            throw new DomainException(ErrorCode.CONFLICT, "Ja existe uma conta para o email informado.");
        }

        Set<Role> roles = (command.roles() == null || command.roles().isEmpty())
                ? Set.of(Role.MEMBER)
                : Set.copyOf(command.roles());

        Instant now = clock.now();
        User newUser = User.pendingActivation(
                UserId.newId(),
                email,
                command.displayName(),
                roles,
                actor.id(),
                now
        );
        User savedUser = userStore.save(newUser);

        String rawToken = tokenGenerator.generateToken();
        Instant expiresAt = now.plus(activationTtl);
        Invitation invitation = Invitation.create(
                UUID.randomUUID(),
                savedUser.id(),
                InvitationPurpose.ACCOUNT_ACTIVATION,
                tokenGenerator.hash(rawToken),
                now,
                expiresAt
        );
        invitationStore.save(invitation);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                actor.id().value(),
                savedUser.id().value(),
                AuditAction.USER_CREATED,
                "SUCCESS",
                "Conta gerenciada criada com convite de ativacao.",
                now
        ));

        return new CreatedManagedUser(savedUser, rawToken, expiresAt);
    }
}
