package io.github.jvpgjava.creatingj.task.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record ToolApprovalId(UUID value) {

    public ToolApprovalId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da aprovacao obrigatorio.");
        }
    }

    public static ToolApprovalId random() {
        return new ToolApprovalId(UUID.randomUUID());
    }

    public static ToolApprovalId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da aprovacao obrigatorio.");
        }
        try {
            return new ToolApprovalId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da aprovacao invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
