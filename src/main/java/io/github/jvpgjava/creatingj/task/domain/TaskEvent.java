package io.github.jvpgjava.creatingj.task.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskEvent(
        UUID id,
        TaskId taskId,
        String eventKey,
        TaskEventType eventType,
        String message,
        Instant createdAt
) {

    public TaskEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(createdAt, "createdAt");
        if (eventKey == null || eventKey.isBlank() || eventKey.trim().length() > 120) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Chave de evento obrigatoria.");
        }
        if (message == null || message.isBlank() || message.trim().length() > 1000) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Mensagem de evento invalida.");
        }
        eventKey = eventKey.trim();
        message = message.trim();
    }
}
