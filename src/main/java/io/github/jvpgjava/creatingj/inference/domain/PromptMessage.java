package io.github.jvpgjava.creatingj.inference.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

public record PromptMessage(ChatRole role, String content) {

    public PromptMessage {
        if (role == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Role da mensagem obrigatoria.");
        }
        if (content == null || content.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo da mensagem obrigatorio.");
        }
    }
}
