package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public record UserSession(
        UUID id,
        UserId userId,
        String clientId,
        String authMethod,
        UUID familyId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        String refreshTokenHash,
        Instant refreshExpiresAt,
        Instant rotatedAt
) {

    public UserSession {
        if (id == null || userId == null || familyId == null || createdAt == null || expiresAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de sessao ausentes.");
        }
        if (clientId == null || clientId.isBlank() || authMethod == null || authMethod.isBlank()
                || tokenHash == null || tokenHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Metadados de sessao obrigatorios.");
        }
    }

    public static UserSession create(
            UUID id,
            UserId userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new UserSession(id, userId, "api", "PASSWORD", id, tokenHash, createdAt, expiresAt, null,
                null, null, null);
    }

    public static UserSession createForClient(
            UUID id,
            UserId userId,
            String clientId,
            String authMethod,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new UserSession(id, userId, clientId, authMethod, id, tokenHash, createdAt, expiresAt, null,
                null, null, null);
    }

    public static UserSession createWithRefresh(
            UUID id,
            UserId userId,
            String clientId,
            String authMethod,
            UUID familyId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            String refreshTokenHash,
            Instant refreshExpiresAt
    ) {
        if (refreshTokenHash == null || refreshTokenHash.isBlank() || refreshExpiresAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Metadados de refresh token obrigatorios.");
        }
        return new UserSession(id, userId, clientId, authMethod, familyId, tokenHash, createdAt, expiresAt, null,
                refreshTokenHash, refreshExpiresAt, null);
    }

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && rotatedAt == null && expiresAt.isAfter(now);
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }

    public boolean isRefreshableAt(Instant now) {
        return revokedAt == null
                && rotatedAt == null
                && refreshTokenHash != null
                && refreshExpiresAt != null
                && refreshExpiresAt.isAfter(now);
    }

    public UserSession revoke(Instant now) {
        return new UserSession(id, userId, clientId, authMethod, familyId, tokenHash, createdAt, expiresAt, now,
                refreshTokenHash, refreshExpiresAt, rotatedAt);
    }

    public UserSession markRotated(Instant now) {
        return new UserSession(id, userId, clientId, authMethod, familyId, tokenHash, createdAt, expiresAt, revokedAt,
                refreshTokenHash, refreshExpiresAt, now);
    }
}
