package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record KnowledgeSourceId(UUID value) {

    public KnowledgeSourceId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da fonte obrigatorio.");
        }
    }

    public static KnowledgeSourceId random() {
        return new KnowledgeSourceId(UUID.randomUUID());
    }

    public static KnowledgeSourceId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da fonte obrigatorio.");
        }
        try {
            return new KnowledgeSourceId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da fonte invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
