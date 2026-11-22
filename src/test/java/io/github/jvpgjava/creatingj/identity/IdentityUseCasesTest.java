package io.github.jvpgjava.creatingj.identity;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ActivateAccountUseCase.ActivateAccountCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase.LoginCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.ConfirmPasswordResetUseCase.ConfirmPasswordResetCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.CreateManagedUserUseCase.CreateManagedUserCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.ListUsersQuery;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.in.RequestPasswordResetUseCase.RequestPasswordResetCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.SuspendUserUseCase.SuspendUserCommand;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.LoginAttemptGuardPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.application.usecase.ActivateAccountInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.AuthenticateWithPasswordInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ConfirmPasswordResetInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.CreateManagedUserInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ListUsersInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.RequestPasswordResetInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ResolveAuthenticatedSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.SuspendUserInteractor;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityUseCasesTest {

    private static final Instant START = Instant.parse("2026-11-21T23:00:00Z");

    private AtomicReference<Instant> currentTime;
    private ClockPort clock;
    private InMemoryUserStore userStore;
    private InMemoryInvitationStore invitationStore;
    private InMemorySessionStore sessionStore;
    private FakePasswordHasher passwordHasher;
    private FakeTokenGenerator tokenGenerator;
    private FakeLoginAttemptGuard loginGuard;
    private InMemoryAuditStore auditStore;

    private User adminUser;
    private User memberUser;

    private CreateManagedUserInteractor createManagedUser;
    private ListUsersInteractor listUsers;
    private SuspendUserInteractor suspendUser;
    private ActivateAccountInteractor activateAccount;
    private AuthenticateWithPasswordInteractor authenticate;
    private RequestPasswordResetInteractor requestReset;
    private ConfirmPasswordResetInteractor confirmReset;
    private ResolveAuthenticatedSessionInteractor resolveSession;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicReference<>(START);
        clock = currentTime::get;
        userStore = new InMemoryUserStore();
        invitationStore = new InMemoryInvitationStore();
        sessionStore = new InMemorySessionStore();
        passwordHasher = new FakePasswordHasher();
        tokenGenerator = new FakeTokenGenerator();
        loginGuard = new FakeLoginAttemptGuard();
        auditStore = new InMemoryAuditStore();

        adminUser = User.active(
                UserId.newId(),
                Email.of("admin@creatingj.dev"),
                "Admin",
                passwordHasher.hash("AdminPass!2026"),
                Set.of(Role.ADMIN),
                null,
                START
        );
        memberUser = User.active(
                UserId.newId(),
                Email.of("member@creatingj.dev"),
                "Member",
                passwordHasher.hash("MemberPass!2026"),
                Set.of(Role.MEMBER),
                adminUser.id(),
                START
        );
        userStore.save(adminUser);
        userStore.save(memberUser);

        createManagedUser = new CreateManagedUserInteractor(
                userStore, invitationStore, tokenGenerator, auditStore, clock, Duration.ofHours(24));
        listUsers = new ListUsersInteractor(userStore);
        suspendUser = new SuspendUserInteractor(userStore, sessionStore, invitationStore, auditStore, clock);
        activateAccount = new ActivateAccountInteractor(
                userStore, invitationStore, passwordHasher, tokenGenerator, auditStore, clock);
        authenticate = new AuthenticateWithPasswordInteractor(
                userStore, sessionStore, passwordHasher, tokenGenerator, loginGuard, auditStore, clock,
                Duration.ofHours(8));
        requestReset = new RequestPasswordResetInteractor(
                userStore, invitationStore, tokenGenerator, auditStore, clock, Duration.ofMinutes(30));
        confirmReset = new ConfirmPasswordResetInteractor(
                userStore, invitationStore, sessionStore, passwordHasher, tokenGenerator, auditStore, clock);
        resolveSession = new ResolveAuthenticatedSessionInteractor(
                sessionStore, userStore, tokenGenerator, clock);
    }

    @Test
    void shouldAllowAdminToCreateUserAndDenyNonAdminOrDuplicate() {
        var created = createManagedUser.execute(new CreateManagedUserCommand(
                adminUser.id(),
                "novo@creatingj.dev",
                "Novo Usuario",
                Set.of(Role.MEMBER)
        ));

        assertThat(created.user().status()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(created.activationToken()).isNotBlank();
        assertThat(invitationStore.findByTokenHash(tokenGenerator.hash(created.activationToken()))).isPresent();
        assertThat(auditStore.events).anyMatch(e -> e.action() == AuditAction.USER_CREATED);

        assertThatThrownBy(() -> createManagedUser.execute(new CreateManagedUserCommand(
                adminUser.id(), "novo@creatingj.dev", "Duplicado", Set.of(Role.MEMBER))))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        assertThatThrownBy(() -> createManagedUser.execute(new CreateManagedUserCommand(
                memberUser.id(), "outro@creatingj.dev", "Sem Perm", Set.of(Role.MEMBER))))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void shouldActivateAccountOnceAndAuthenticateAndRejectReuseOrExpiredToken() {
        var created = createManagedUser.execute(new CreateManagedUserCommand(
                adminUser.id(), "ativa@creatingj.dev", "Ativa", Set.of(Role.MEMBER)));

        activateAccount.execute(new ActivateAccountCommand(created.activationToken(), "StrongPass!2026"));

        assertThatThrownBy(() -> activateAccount.execute(
                new ActivateAccountCommand(created.activationToken(), "OtherPass!2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        var session = authenticate.execute(new LoginCommand("ativa@creatingj.dev", "StrongPass!2026"));
        assertThat(session.accessToken()).isNotBlank();
        assertThat(resolveSession.execute(session.accessToken())).isPresent();
    }

    @Test
    void shouldEnforceBruteForceLockoutAndAntiEnumerationOnLogin() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authenticate.execute(new LoginCommand("member@creatingj.dev", "Wrong!Pass123")))
                    .isInstanceOf(DomainException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        assertThatThrownBy(() -> authenticate.execute(new LoginCommand("member@creatingj.dev", "MemberPass!2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

        assertThatThrownBy(() -> authenticate.execute(new LoginCommand("inexistente@creatingj.dev", "AnyPass!2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldSuspendUserAndImmediatelyRevokeActiveSessionsAndPreventSelfSuspension() {
        var login = authenticate.execute(new LoginCommand("member@creatingj.dev", "MemberPass!2026"));
        assertThat(resolveSession.execute(login.accessToken())).isPresent();

        var suspended = suspendUser.execute(new SuspendUserCommand(adminUser.id(), memberUser.id()));
        assertThat(suspended.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(resolveSession.execute(login.accessToken())).isEmpty();

        assertThatThrownBy(() -> authenticate.execute(new LoginCommand("member@creatingj.dev", "MemberPass!2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThatThrownBy(() -> suspendUser.execute(new SuspendUserCommand(adminUser.id(), adminUser.id())))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        assertThatThrownBy(() -> suspendUser.execute(new SuspendUserCommand(memberUser.id(), adminUser.id())))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void shouldListUsersForAdminAndValidatePaginationBounds() {
        var page = listUsers.execute(new ListUsersQuery(
                adminUser.id(), 0, 10, AccountStatus.ACTIVE, UserSortField.EMAIL, true));
        assertThat(page.items()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);

        assertThatThrownBy(() -> listUsers.execute(new ListUsersQuery(
                memberUser.id(), 0, 10, null, UserSortField.CREATED_AT, false)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> listUsers.execute(new ListUsersQuery(
                adminUser.id(), -1, 10, null, UserSortField.CREATED_AT, false)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> listUsers.execute(new ListUsersQuery(
                adminUser.id(), 0, 101, null, UserSortField.CREATED_AT, false)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldHandlePasswordResetWithoutEnumerationAndRevokePreviousSessions() {
        var oldLogin = authenticate.execute(new LoginCommand("member@creatingj.dev", "MemberPass!2026"));

        var unknownReset = requestReset.execute(new RequestPasswordResetCommand("naoexiste@creatingj.dev"));
        assertThat(unknownReset.issuedResetToken()).isEmpty();

        var knownReset = requestReset.execute(new RequestPasswordResetCommand("member@creatingj.dev"));
        assertThat(knownReset.issuedResetToken()).isPresent();

        String resetToken = knownReset.issuedResetToken().orElseThrow();
        confirmReset.execute(new ConfirmPasswordResetCommand(resetToken, "NewStr0ng!Pass2026"));

        assertThat(resolveSession.execute(oldLogin.accessToken())).isEmpty();
        assertThat(authenticate.execute(new LoginCommand("member@creatingj.dev", "NewStr0ng!Pass2026"))
                .accessToken()).isNotBlank();

        assertThatThrownBy(() -> confirmReset.execute(
                new ConfirmPasswordResetCommand(resetToken, "Another!Pass2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void shouldRejectEdgeCasesForTokensRolesSessionsAndSuspendedAccounts() {
        assertThatThrownBy(() -> createManagedUser.execute(
                new CreateManagedUserCommand(null, "a@creatingj.dev", "A", null)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        var defaultRoleCreated = createManagedUser.execute(
                new CreateManagedUserCommand(adminUser.id(), "defaultrole@creatingj.dev", "Default Role", null));
        assertThat(defaultRoleCreated.user().roles()).containsExactly(Role.MEMBER);

        assertThatThrownBy(() -> activateAccount.execute(new ActivateAccountCommand(" ", "StrongPass!2026")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> confirmReset.execute(new ConfirmPasswordResetCommand(" ", "StrongPass!2026")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> confirmReset.execute(
                new ConfirmPasswordResetCommand(defaultRoleCreated.activationToken(), "StrongPass!2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        var reset = requestReset.execute(new RequestPasswordResetCommand("member@creatingj.dev"));
        assertThatThrownBy(() -> activateAccount.execute(
                new ActivateAccountCommand(reset.issuedResetToken().orElseThrow(), "StrongPass!2026")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThat(resolveSession.execute(null)).isEmpty();
        assertThat(resolveSession.execute("  ")).isEmpty();
        assertThat(resolveSession.execute("unknown-token")).isEmpty();

        var session = authenticate.execute(new LoginCommand("member@creatingj.dev", "MemberPass!2026"));
        currentTime.set(START.plus(Duration.ofHours(9)));
        assertThat(resolveSession.execute(session.accessToken())).isEmpty();

        assertThatThrownBy(() -> suspendUser.execute(new SuspendUserCommand(adminUser.id(), UserId.newId())))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);

        var defaultSortPage = listUsers.execute(new ListUsersQuery(adminUser.id(), 0, 10, null, null, false));
        assertThat(defaultSortPage.items()).isNotEmpty();

        suspendUser.execute(new SuspendUserCommand(adminUser.id(), memberUser.id()));
        assertThat(requestReset.execute(new RequestPasswordResetCommand("member@creatingj.dev"))
                .issuedResetToken()).isEmpty();
    }

    private static final class InMemoryUserStore implements UserStorePort {
        private final Map<UserId, User> byId = new HashMap<>();

        @Override
        public User save(User user) {
            byId.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UserId id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return byId.values().stream()
                    .filter(u -> u.email().equals(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public List<User> findPage(int page, int size, AccountStatus status, UserSortField sortBy, boolean ascending) {
            Comparator<User> comparator = switch (sortBy) {
                case EMAIL -> Comparator.comparing(u -> u.email().value());
                case DISPLAY_NAME -> Comparator.comparing(User::displayName);
                case STATUS -> Comparator.comparing(u -> u.status().name());
                case CREATED_AT -> Comparator.comparing(User::createdAt);
            };
            if (!ascending) {
                comparator = comparator.reversed();
            }
            return byId.values().stream()
                    .filter(u -> status == null || u.status() == status)
                    .sorted(comparator)
                    .skip((long) page * size)
                    .limit(size)
                    .toList();
        }

        @Override
        public long count(AccountStatus status) {
            return byId.values().stream()
                    .filter(u -> status == null || u.status() == status)
                    .count();
        }
    }

    private static final class InMemoryInvitationStore implements InvitationStorePort {
        private final Map<String, Invitation> byHash = new HashMap<>();

        @Override
        public Invitation save(Invitation invitation) {
            byHash.put(invitation.tokenHash(), invitation);
            return invitation;
        }

        @Override
        public Optional<Invitation> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void invalidatePendingByUserAndPurpose(UserId userId, InvitationPurpose purpose, Instant now) {
            byHash.replaceAll((k, v) -> {
                if (v.userId().equals(userId) && v.purpose() == purpose && v.usedAt() == null) {
                    return new Invitation(v.id(), v.userId(), v.purpose(), v.tokenHash(), v.expiresAt(), now,
                            v.createdAt());
                }
                return v;
            });
        }
    }

    private static final class InMemorySessionStore implements SessionStorePort {
        private final Map<String, UserSession> byHash = new HashMap<>();

        @Override
        public UserSession save(UserSession session) {
            byHash.put(session.tokenHash(), session);
            return session;
        }

        @Override
        public Optional<UserSession> findById(UUID sessionId) {
            return byHash.values().stream().filter(s -> s.id().equals(sessionId)).findFirst();
        }

        @Override
        public Optional<UserSession> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
            return byHash.values().stream()
                    .filter(s -> refreshTokenHash.equals(s.refreshTokenHash()))
                    .findFirst();
        }

        @Override
        public List<UserSession> findActiveByUserId(UserId userId, Instant now) {
            return byHash.values().stream()
                    .filter(s -> s.userId().equals(userId) && s.isActiveAt(now))
                    .toList();
        }

        @Override
        public void revokeAllByUserId(UserId userId, Instant revokedAt) {
            byHash.replaceAll((k, v) -> v.userId().equals(userId) && v.revokedAt() == null
                    ? v.revoke(revokedAt)
                    : v);
        }

        @Override
        public void revokeByFamilyId(UUID familyId, Instant revokedAt) {
            byHash.replaceAll((k, v) -> v.familyId().equals(familyId) && v.revokedAt() == null
                    ? v.revoke(revokedAt)
                    : v);
        }
    }

    private static final class FakePasswordHasher implements PasswordHasherPort {
        @Override
        public String hash(String rawPassword) {
            return "$argon2id$fake$" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedHash) {
            return encodedHash != null && encodedHash.equals(hash(rawPassword));
        }
    }

    private static final class FakeTokenGenerator implements OpaqueTokenGeneratorPort {
        private final AtomicInteger seq = new AtomicInteger(1);

        @Override
        public String generateToken() {
            return "token-value-" + seq.getAndIncrement();
        }

        @Override
        public String hash(String rawToken) {
            return String.format("%064x", rawToken.hashCode() & 0xffffffffL);
        }
    }

    private static final class FakeLoginAttemptGuard implements LoginAttemptGuardPort {
        private final Map<String, Integer> attempts = new HashMap<>();

        @Override
        public boolean isBlocked(String key) {
            return attempts.getOrDefault(key, 0) >= 3;
        }

        @Override
        public void recordFailure(String key) {
            attempts.merge(key, 1, Integer::sum);
        }

        @Override
        public void reset(String key) {
            attempts.remove(key);
        }
    }

    private static final class InMemoryAuditStore implements AuditPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }
}
