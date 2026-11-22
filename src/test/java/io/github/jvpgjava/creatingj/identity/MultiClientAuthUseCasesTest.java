package io.github.jvpgjava.creatingj.identity;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthorizePkceUseCase.AuthorizePkceCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase.ExchangePkceCodeCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.in.LogoutSessionUseCase.LogoutSessionCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.RefreshSessionUseCase.RefreshSessionCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.RevokeOwnSessionUseCase.RevokeOwnSessionCommand;
import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.application.usecase.AuthorizePkceInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ExchangePkceCodeInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.GetCurrentUserInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ListOwnSessionsInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.LogoutSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.RefreshSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.RevokeOwnSessionInteractor;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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

class MultiClientAuthUseCasesTest {

    private static final Instant START = Instant.parse("2026-11-21T23:55:00Z");

    private AtomicReference<Instant> currentTime;
    private ClockPort clock;
    private InMemoryUserStore userStore;
    private InMemorySessionStore sessionStore;
    private InMemoryAuthCodeStore authCodeStore;
    private FakeTokenGenerator tokenGenerator;
    private InMemoryAuditStore auditStore;

    private User userA;
    private User userB;

    private AuthorizePkceInteractor authorizePkce;
    private ExchangePkceCodeInteractor exchangePkce;
    private RefreshSessionInteractor refreshSession;
    private GetCurrentUserInteractor getCurrentUser;
    private LogoutSessionInteractor logoutSession;
    private ListOwnSessionsInteractor listOwnSessions;
    private RevokeOwnSessionInteractor revokeOwnSession;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicReference<>(START);
        clock = currentTime::get;
        userStore = new InMemoryUserStore();
        sessionStore = new InMemorySessionStore();
        authCodeStore = new InMemoryAuthCodeStore();
        tokenGenerator = new FakeTokenGenerator();
        auditStore = new InMemoryAuditStore();

        userA = User.active(UserId.newId(), Email.of("usera@creatingj.dev"), "User A", "$argon2id$a",
                Set.of(Role.MEMBER), null, START);
        userB = User.active(UserId.newId(), Email.of("userb@creatingj.dev"), "User B", "$argon2id$b",
                Set.of(Role.MEMBER), null, START);
        userStore.save(userA);
        userStore.save(userB);

        authorizePkce = new AuthorizePkceInteractor(
                userStore, authCodeStore, tokenGenerator, auditStore, clock, Duration.ofMinutes(2));
        exchangePkce = new ExchangePkceCodeInteractor(
                userStore, authCodeStore, sessionStore, tokenGenerator, auditStore, clock,
                Duration.ofMinutes(15), Duration.ofDays(14));
        refreshSession = new RefreshSessionInteractor(
                userStore, sessionStore, tokenGenerator, auditStore, clock,
                Duration.ofMinutes(15), Duration.ofDays(14));
        getCurrentUser = new GetCurrentUserInteractor(userStore, sessionStore, clock);
        logoutSession = new LogoutSessionInteractor(sessionStore, auditStore, clock);
        listOwnSessions = new ListOwnSessionsInteractor(sessionStore, clock);
        revokeOwnSession = new RevokeOwnSessionInteractor(sessionStore, auditStore, clock);
    }

    @Test
    void shouldAuthorizeExchangeRotateAndDetectRefreshReuse() throws Exception {
        String verifier = "vErIfIeR-value-with-enough-length-for-rfc7636-compliance-12345";
        String challenge = sha256Base64Url(verifier);

        var auth = authorizePkce.execute(new AuthorizePkceCommand(
                userA.id(), "creatingj-vscode", "http://127.0.0.1/callback", challenge, "S256", "state-123"));
        assertThat(auth.authorizationCode()).isNotBlank();
        assertThat(auth.state()).isEqualTo("state-123");

        assertThatThrownBy(() -> exchangePkce.execute(new ExchangePkceCodeCommand(
                auth.authorizationCode(), "creatingj-vscode", "http://127.0.0.1/callback", verifier + "wrong")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        var tokens = exchangePkce.execute(new ExchangePkceCodeCommand(
                auth.authorizationCode(), "creatingj-vscode", "http://127.0.0.1/callback", verifier));
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        assertThatThrownBy(() -> exchangePkce.execute(new ExchangePkceCodeCommand(
                auth.authorizationCode(), "creatingj-vscode", "http://127.0.0.1/callback", verifier)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        var rotated = refreshSession.execute(new RefreshSessionCommand(tokens.refreshToken()));
        assertThat(rotated.accessToken()).isNotEqualTo(tokens.accessToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(tokens.refreshToken());

        assertThatThrownBy(() -> refreshSession.execute(new RefreshSessionCommand(tokens.refreshToken())))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThat(auditStore.events).anyMatch(e -> e.action() == AuditAction.REFRESH_REUSE_DETECTED);
        assertThat(listOwnSessions.execute(userA.id())).isEmpty();
    }

    @Test
    void shouldEnforceCrossUserSessionIsolationAndLogout() {
        UserSession sessionA = UserSession.create(
                UUID.randomUUID(), userA.id(), tokenGenerator.hash("tok-a"), START, START.plusSeconds(3600));
        UserSession sessionB = UserSession.create(
                UUID.randomUUID(), userB.id(), tokenGenerator.hash("tok-b"), START, START.plusSeconds(3600));
        sessionStore.save(sessionA);
        sessionStore.save(sessionB);

        var meA = getCurrentUser.execute(userA.id(), sessionA.id());
        assertThat(meA.user().email().value()).isEqualTo("usera@creatingj.dev");
        assertThat(meA.session().id()).isEqualTo(sessionA.id());

        assertThat(listOwnSessions.execute(userA.id())).extracting(UserSession::id).containsExactly(sessionA.id());

        assertThatThrownBy(() -> revokeOwnSession.execute(new RevokeOwnSessionCommand(userA.id(), sessionB.id())))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(sessionStore.findById(sessionB.id()).orElseThrow().isActiveAt(START)).isTrue();

        logoutSession.execute(new LogoutSessionCommand(userA.id(), sessionA.id()));
        assertThat(sessionStore.findById(sessionA.id()).orElseThrow().isActiveAt(START)).isFalse();
        assertThat(auditStore.events).anyMatch(e -> e.action() == AuditAction.LOGOUT);

        revokeOwnSession.execute(new RevokeOwnSessionCommand(userB.id(), sessionB.id()));
        assertThat(sessionStore.findById(sessionB.id()).orElseThrow().isActiveAt(START)).isFalse();
    }

    @Test
    void shouldRejectRefreshAndPkceForSuspendedUsersAndInvalidInputs() throws Exception {
        String verifier = "vErIfIeR-value-with-enough-length-for-rfc7636-compliance-99999";
        String challenge = sha256Base64Url(verifier);
        User suspended = userA.suspend(START);
        userStore.save(suspended);

        assertThatThrownBy(() -> authorizePkce.execute(new AuthorizePkceCommand(
                userA.id(), "creatingj-vscode", "http://127.0.0.1/callback", challenge, "S256", "s")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThatThrownBy(() -> refreshSession.execute(new RefreshSessionCommand(" ")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThatThrownBy(() -> exchangePkce.execute(new ExchangePkceCodeCommand(
                " ", "creatingj-vscode", "http://127.0.0.1/callback", verifier)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private static String sha256Base64Url(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static final class InMemoryUserStore implements UserStorePort {
        private final Map<UserId, User> map = new HashMap<>();

        @Override
        public User save(User user) {
            map.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UserId id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return map.values().stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public List<User> findPage(int page, int size, AccountStatus status, UserSortField sortBy, boolean ascending) {
            return List.copyOf(map.values());
        }

        @Override
        public long count(AccountStatus status) {
            return map.size();
        }
    }

    private static final class InMemorySessionStore implements SessionStorePort {
        private final Map<UUID, UserSession> byId = new HashMap<>();

        @Override
        public UserSession save(UserSession session) {
            byId.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<UserSession> findById(UUID sessionId) {
            return Optional.ofNullable(byId.get(sessionId));
        }

        @Override
        public Optional<UserSession> findByTokenHash(String tokenHash) {
            return byId.values().stream().filter(s -> s.tokenHash().equals(tokenHash)).findFirst();
        }

        @Override
        public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
            return byId.values().stream()
                    .filter(s -> refreshTokenHash.equals(s.refreshTokenHash()))
                    .findFirst();
        }

        @Override
        public List<UserSession> findActiveByUserId(UserId userId, Instant now) {
            return byId.values().stream()
                    .filter(s -> s.userId().equals(userId) && s.isActiveAt(now))
                    .toList();
        }

        @Override
        public void revokeAllByUserId(UserId userId, Instant revokedAt) {
            byId.replaceAll((k, v) -> v.userId().equals(userId) && v.revokedAt() == null ? v.revoke(revokedAt) : v);
        }

        @Override
        public void revokeByFamilyId(UUID familyId, Instant revokedAt) {
            byId.replaceAll((k, v) -> v.familyId().equals(familyId) && v.revokedAt() == null
                    ? v.revoke(revokedAt)
                    : v);
        }
    }

    private static final class InMemoryAuthCodeStore implements AuthorizationCodeStorePort {
        private final Map<String, AuthorizationCode> byHash = new HashMap<>();

        @Override
        public AuthorizationCode save(AuthorizationCode code) {
            byHash.put(code.codeHash(), code);
            return code;
        }

        @Override
        public Optional<AuthorizationCode> findByCodeHash(String codeHash) {
            return Optional.ofNullable(byHash.get(codeHash));
        }
    }

    private static final class FakeTokenGenerator implements OpaqueTokenGeneratorPort {
        private final AtomicInteger seq = new AtomicInteger(1);

        @Override
        public String generateToken() {
            return "tok-" + seq.getAndIncrement() + "-random-value";
        }

        @Override
        public String hash(String rawToken) {
            return String.format("%064x", rawToken.hashCode() & 0xffffffffL);
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
