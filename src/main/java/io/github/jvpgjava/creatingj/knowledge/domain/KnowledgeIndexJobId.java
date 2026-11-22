package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record KnowledgeIndexJobId(UUID value) {

    public KnowledgeIndexJobId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do job obrigatorio.");
        }
    }

    public static KnowledgeIndexJobId random() {
        return new KnowledgeIndexJobId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
