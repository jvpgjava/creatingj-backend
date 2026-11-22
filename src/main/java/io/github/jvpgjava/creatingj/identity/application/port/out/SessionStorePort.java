package io.github.jvpgjava.creatingj.identity.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionStorePort {

    UserSession save(UserSession session);

    Optional<UserSession> findById(UUID sessionId);

    Optional<UserSession> findByTokenHash(String tokenHash);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findActiveByUserId(UserId userId, Instant now);

    void revokeAllByUserId(UserId userId, Instant revokedAt);

    void revokeByFamilyId(UUID familyId, Instant revokedAt);
}
