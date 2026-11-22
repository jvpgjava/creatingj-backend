package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public record Invitation(
        UUID id,
        UserId userId,
        InvitationPurpose purpose,
        String tokenHash,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt
) {

    public Invitation {
        if (id == null || userId == null || purpose == null || expiresAt == null || createdAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de convite ausentes.");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Hash de token obrigatorio.");
        }
    }

    public static Invitation create(
            UUID id,
            UserId userId,
            InvitationPurpose purpose,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new Invitation(id, userId, purpose, tokenHash, expiresAt, null, createdAt);
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && !isExpiredAt(now);
    }

    public Invitation markUsed(Instant now) {
        if (usedAt != null) {
            throw new DomainException(ErrorCode.CONFLICT, "Token ja utilizado.");
        }
        if (isExpiredAt(now)) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Token expirado.");
        }
        return new Invitation(id, userId, purpose, tokenHash, expiresAt, now, createdAt);
    }
}
