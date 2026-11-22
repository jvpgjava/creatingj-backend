package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public record AuthorizationCode(
        UUID id,
        UserId userId,
        String clientId,
        String redirectUri,
        String codeHash,
        PkceChallenge challenge,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt
) {

    public AuthorizationCode {
        if (id == null || userId == null || challenge == null || createdAt == null || expiresAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de authorizationCode ausentes.");
        }
        if (clientId == null || clientId.isBlank() || redirectUri == null || redirectUri.isBlank()
                || codeHash == null || codeHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Metadados de authorizationCode invalidos.");
        }
    }

    public static AuthorizationCode create(
            UUID id,
            UserId userId,
            String clientId,
            String redirectUri,
            String codeHash,
            PkceChallenge challenge,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new AuthorizationCode(id, userId, clientId, redirectUri, codeHash, challenge, createdAt, expiresAt,
                null);
    }

    public boolean matchesClientAndRedirect(String candidateClientId, String candidateRedirectUri) {
        return clientId.equals(candidateClientId) && redirectUri.equals(candidateRedirectUri);
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public AuthorizationCode markUsed(Instant now) {
        if (usedAt != null) {
            throw new DomainException(ErrorCode.CONFLICT, "Codigo de autorizacao ja utilizado.");
        }
        if (isExpiredAt(now)) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Codigo de autorizacao expirado.");
        }
        return new AuthorizationCode(id, userId, clientId, redirectUri, codeHash, challenge, createdAt, expiresAt, now);
    }
}
