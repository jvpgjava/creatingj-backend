package io.github.jvpgjava.creatingj.audit.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        UUID actorUserId,
        UUID targetUserId,
        AuditAction action,
        String outcome,
        String summary,
        Instant occurredAt
) {

    public AuditEvent {
        if (id == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "id de auditoria obrigatorio");
        }
        if (action == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "acao de auditoria obrigatoria");
        }
        if (outcome == null || outcome.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "outcome de auditoria obrigatorio");
        }
        if (summary == null || summary.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "summary de auditoria obrigatorio");
        }
        if (occurredAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "occurredAt de auditoria obrigatorio");
        }
    }

    public static AuditEvent of(
            UUID id,
            UUID actorUserId,
            UUID targetUserId,
            AuditAction action,
            String outcome,
            String summary,
            Instant occurredAt
    ) {
        return new AuditEvent(id, actorUserId, targetUserId, action, outcome, summary, occurredAt);
    }
}
