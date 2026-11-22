package io.github.jvpgjava.creatingj.task.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record TaskId(UUID value) {

    public TaskId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da tarefa obrigatorio.");
        }
    }

    public static TaskId random() {
        return new TaskId(UUID.randomUUID());
    }

    public static TaskId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da tarefa obrigatorio.");
        }
        try {
            return new TaskId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da tarefa invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
