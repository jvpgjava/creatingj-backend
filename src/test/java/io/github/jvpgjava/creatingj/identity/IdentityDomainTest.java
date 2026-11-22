package io.github.jvpgjava.creatingj.identity;

import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.PasswordPolicy;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-21T23:30:00Z");

    @Test
    void shouldNormalizeAndValidateEmail() {
        Email email = Email.of("  Joao.Admin@CreatingJ.dev  ");
        assertThat(email.value()).isEqualTo("joao.admin@creatingj.dev");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "invalid", "a@", "@domain.com", "user@domain", "a b@creatingj.dev"})
    void shouldRejectInvalidEmail(String raw) {
        assertThatThrownBy(() -> Email.of(raw))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldRejectNullOrOversizedEmail() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(DomainException.class);
        String longEmail = "a".repeat(250) + "@b.co";
        assertThatThrownBy(() -> Email.of(longEmail))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldValidateUserIdParsing() {
        UserId generated = UserId.newId();
        assertThat(UserId.fromString(generated.value().toString())).isEqualTo(generated);
        assertThatThrownBy(() -> UserId.fromString(null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserId.fromString("   "))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserId.fromString("not-a-uuid"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldEnforcePasswordPolicy() {
        assertThatCode(() -> PasswordPolicy.validate("Str0ng!Pass2026")).doesNotThrowAnyException();

        assertThatThrownBy(() -> PasswordPolicy.validate(null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("Short1!a")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("A".repeat(125) + "a1!#")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("lowercase123!@#")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("UPPERCASE123!@#")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("NoDigitsHere!@#")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("NoSymbols123456")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("Bad\nChar12345!"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldTransitionUserLifecycleSafely() {
        UserId adminId = UserId.newId();
        User pending = User.pendingActivation(
                UserId.newId(),
                Email.of("member@creatingj.dev"),
                "Maria Silva",
                Set.of(Role.MEMBER),
                adminId,
                NOW
        );

        assertThat(pending.status()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(pending.isActive()).isFalse();
        assertThat(pending.isAdmin()).isFalse();

        User active = pending.activate("$argon2id$hash", NOW.plusSeconds(60));
        assertThat(active.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(active.isActive()).isTrue();
        assertThat(active.passwordHash()).isEqualTo("$argon2id$hash");

        User updatedPass = active.changePassword("$argon2id$newHash", NOW.plusSeconds(120));
        assertThat(updatedPass.passwordHash()).isEqualTo("$argon2id$newHash");

        User suspended = updatedPass.suspend(NOW.plusSeconds(180));
        assertThat(suspended.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(suspended.suspendedAt()).isEqualTo(NOW.plusSeconds(180));
        assertThat(suspended.isActive()).isFalse();

        assertThatThrownBy(() -> suspended.suspend(NOW.plusSeconds(200)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATE);

        assertThatThrownBy(() -> suspended.activate("$argon2id$x", NOW.plusSeconds(200)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATE);

        assertThatThrownBy(() -> suspended.changePassword("$argon2id$x", NOW.plusSeconds(200)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATE);

        assertThat(active.hasRole(Role.MEMBER)).isTrue();
        assertThat(active.hasRole(Role.ADMIN)).isFalse();
        assertThatThrownBy(() -> pending.activate(" ", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> active.changePassword(" ", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> User.active(UserId.newId(), Email.of("a@b.dev"), "Name", " ", Set.of(Role.MEMBER),
                null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> User.pendingActivation(UserId.newId(), Email.of("a@b.dev"), " ", Set.of(Role.MEMBER),
                null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> User.pendingActivation(UserId.newId(), Email.of("a@b.dev"), "A".repeat(121),
                Set.of(Role.MEMBER), null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> User.pendingActivation(UserId.newId(), Email.of("a@b.dev"), "Nome", Set.of(),
                null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new User(null, Email.of("a@b.dev"), "Nome", null, AccountStatus.PENDING_ACTIVATION,
                Set.of(Role.MEMBER), null, NOW, NOW, null)).isInstanceOf(DomainException.class);
    }

    @Test
    void shouldEnforceInvitationSingleUseAndExpiration() {
        Invitation invitation = Invitation.create(
                UUID.randomUUID(),
                UserId.newId(),
                InvitationPurpose.ACCOUNT_ACTIVATION,
                "a".repeat(64),
                NOW,
                NOW.plusSeconds(3600)
        );

        assertThat(invitation.isUsableAt(NOW.plusSeconds(10))).isTrue();
        assertThat(invitation.isExpiredAt(NOW.plusSeconds(3601))).isTrue();
        assertThat(invitation.isUsableAt(NOW.plusSeconds(3601))).isFalse();

        Invitation used = invitation.markUsed(NOW.plusSeconds(20));
        assertThat(used.usedAt()).isEqualTo(NOW.plusSeconds(20));
        assertThat(used.isUsableAt(NOW.plusSeconds(30))).isFalse();

        assertThatThrownBy(() -> used.markUsed(NOW.plusSeconds(30)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        assertThatThrownBy(() -> invitation.markUsed(NOW.plusSeconds(4000)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> Invitation.create(null, UserId.newId(), InvitationPurpose.ACCOUNT_ACTIVATION,
                "a".repeat(64), NOW, NOW.plusSeconds(60))).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Invitation.create(UUID.randomUUID(), UserId.newId(),
                InvitationPurpose.ACCOUNT_ACTIVATION, " ", NOW, NOW.plusSeconds(60)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldHandleSessionExpirationAndRevocation() {
        UserSession session = UserSession.create(
                UUID.randomUUID(),
                UserId.newId(),
                "b".repeat(64),
                NOW,
                NOW.plusSeconds(1800)
        );

        assertThat(session.isActiveAt(NOW.plusSeconds(60))).isTrue();
        assertThat(session.isActiveAt(NOW.plusSeconds(1801))).isFalse();

        UserSession revoked = session.revoke(NOW.plusSeconds(100));
        assertThat(revoked.isActiveAt(NOW.plusSeconds(101))).isFalse();

        assertThatThrownBy(() -> UserSession.create(null, UserId.newId(), "b".repeat(64), NOW, NOW.plusSeconds(60)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> UserSession.create(UUID.randomUUID(), UserId.newId(), " ", NOW, NOW.plusSeconds(60)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldValidateAuditEvent() {
        UUID id = UUID.randomUUID();
        AuditEvent event = AuditEvent.of(
                id,
                UserId.newId().value(),
                UserId.newId().value(),
                AuditAction.USER_CREATED,
                "SUCCESS",
                "Conta criada pelo administrador",
                NOW
        );
        assertThat(event.action()).isEqualTo(AuditAction.USER_CREATED);
        assertThatThrownBy(() -> AuditEvent.of(null, null, null, AuditAction.USER_CREATED, "SUCCESS", "ok", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> AuditEvent.of(id, null, null, null, "SUCCESS", "ok", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> AuditEvent.of(id, null, null, AuditAction.USER_CREATED, " ", "ok", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> AuditEvent.of(id, null, null, AuditAction.USER_CREATED, "SUCCESS", " ", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> AuditEvent.of(id, null, null, AuditAction.USER_CREATED, "SUCCESS", "ok", null))
                .isInstanceOf(DomainException.class);
    }
}
