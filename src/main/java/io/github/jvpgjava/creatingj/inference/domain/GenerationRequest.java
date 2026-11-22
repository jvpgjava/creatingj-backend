package io.github.jvpgjava.creatingj.inference.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.List;

public record GenerationRequest(
        String modelId,
        List<PromptMessage> messages,
        int maxTokens,
        double temperature
) {

    private static final int MAX_ALLOWED_TOKENS = 4096;

    public GenerationRequest {
        if (modelId == null || modelId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Modelo obrigatorio.");
        }
        if (messages == null || messages.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Mensagens obrigatorias.");
        }
        if (maxTokens < 1 || maxTokens > MAX_ALLOWED_TOKENS) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "maxTokens deve estar entre 1 e 4096.");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "temperature deve estar entre 0.0 e 2.0.");
        }
        messages = List.copyOf(messages);
    }
}
