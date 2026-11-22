package io.github.jvpgjava.creatingj.support;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestInMemoryStoresConfig {

    @Bean
    public UserStorePort testUserStorePort() {
        return new UserStorePort() {
            private final Map<UserId, User> users = new ConcurrentHashMap<>();

            @Override
            public User save(User user) {
                users.put(user.id(), user);
                return user;
            }

            @Override
            public Optional<User> findById(UserId id) {
                return Optional.ofNullable(users.get(id));
            }

            @Override
            public Optional<User> findByEmail(Email email) {
                return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
            }

            @Override
            public boolean existsByEmail(Email email) {
                return findByEmail(email).isPresent();
            }

            @Override
            public List<User> findPage(
                    int page,
                    int size,
                    AccountStatus status,
                    UserSortField sortBy,
                    boolean ascending
            ) {
                return users.values().stream()
                        .filter(u -> status == null || u.status() == status)
                        .skip((long) page * size)
                        .limit(size)
                        .toList();
            }

            @Override
            public long count(AccountStatus status) {
                return users.values().stream().filter(u -> status == null || u.status() == status).count();
            }
        };
    }

    @Bean
    public InvitationStorePort testInvitationStorePort() {
        return new InvitationStorePort() {
            private final Map<String, Invitation> store = new ConcurrentHashMap<>();

            @Override
            public Invitation save(Invitation invitation) {
                store.put(invitation.tokenHash(), invitation);
                return invitation;
            }

            @Override
            public Optional<Invitation> findByTokenHash(String tokenHash) {
                return Optional.ofNullable(store.get(tokenHash));
            }

            @Override
            public void invalidatePendingByUserAndPurpose(UserId userId, InvitationPurpose purpose, Instant now) {
                store.replaceAll((k, v) -> v.userId().equals(userId) && v.purpose() == purpose && v.usedAt() == null
                        ? new Invitation(v.id(), v.userId(), v.purpose(), v.tokenHash(), v.expiresAt(), now,
                                v.createdAt())
                        : v);
            }
        };
    }

    @Bean
    public SessionStorePort testSessionStorePort() {
        return new SessionStorePort() {
            private final Map<UUID, UserSession> store = new ConcurrentHashMap<>();

            @Override
            public UserSession save(UserSession session) {
                store.put(session.id(), session);
                return session;
            }

            @Override
            public Optional<UserSession> findById(UUID sessionId) {
                return Optional.ofNullable(store.get(sessionId));
            }

            @Override
            public Optional<UserSession> findByTokenHash(String tokenHash) {
                return store.values().stream().filter(s -> s.tokenHash().equals(tokenHash)).findFirst();
            }

            @Override
            public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
                return store.values().stream()
                        .filter(s -> refreshTokenHash.equals(s.refreshTokenHash()))
                        .findFirst();
            }

            @Override
            public List<UserSession> findActiveByUserId(UserId userId, Instant now) {
                return store.values().stream()
                        .filter(s -> s.userId().equals(userId) && s.isActiveAt(now))
                        .toList();
            }

            @Override
            public void revokeAllByUserId(UserId userId, Instant revokedAt) {
                store.replaceAll((k, v) -> v.userId().equals(userId) && v.revokedAt() == null
                        ? v.revoke(revokedAt)
                        : v);
            }

            @Override
            public void revokeByFamilyId(UUID familyId, Instant revokedAt) {
                store.replaceAll((k, v) -> v.familyId().equals(familyId) && v.revokedAt() == null
                        ? v.revoke(revokedAt)
                        : v);
            }
        };
    }

    @Bean
    public AuthorizationCodeStorePort testAuthorizationCodeStorePort() {
        return new AuthorizationCodeStorePort() {
            private final Map<String, AuthorizationCode> store = new ConcurrentHashMap<>();

            @Override
            public AuthorizationCode save(AuthorizationCode code) {
                store.put(code.codeHash(), code);
                return code;
            }

            @Override
            public Optional<AuthorizationCode> findByCodeHash(String codeHash) {
                return Optional.ofNullable(store.get(codeHash));
            }
        };
    }

    @Bean
    public AuditPort testAuditPort() {
        List<AuditEvent> events = new CopyOnWriteArrayList<>();
        return events::add;
    }
}
