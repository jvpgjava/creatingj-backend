package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record ToolProposalId(UUID value) {

    public ToolProposalId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da proposta obrigatorio.");
        }
    }

    public static ToolProposalId random() {
        return new ToolProposalId(UUID.randomUUID());
    }

    public static ToolProposalId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da proposta obrigatorio.");
        }
        try {
            return new ToolProposalId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da proposta invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
