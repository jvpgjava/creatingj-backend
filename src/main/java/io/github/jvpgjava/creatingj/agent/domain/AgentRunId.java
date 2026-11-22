package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record AgentRunId(UUID value) {

    public AgentRunId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da execucao do agente obrigatorio.");
        }
    }

    public static AgentRunId random() {
        return new AgentRunId(UUID.randomUUID());
    }

    public static AgentRunId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da execucao do agente obrigatorio.");
        }
        try {
            return new AgentRunId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da execucao do agente invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
