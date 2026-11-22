package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record KnowledgeChunkId(UUID value) {

    public KnowledgeChunkId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do chunk obrigatorio.");
        }
    }

    public static KnowledgeChunkId random() {
        return new KnowledgeChunkId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
