package io.github.jvpgjava.creatingj.project.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record ProjectId(UUID value) {

    public ProjectId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do projeto obrigatorio.");
        }
    }

    public static ProjectId random() {
        return new ProjectId(UUID.randomUUID());
    }

    public static ProjectId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do projeto obrigatorio.");
        }
        try {
            return new ProjectId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do projeto invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
