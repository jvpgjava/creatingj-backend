package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIngestionPolicy;

public record WorkspaceFileSnapshot(
        String relativePath,
        String content
) {

    public static final int MAX_FILE_CHARS = 12_000;

    public WorkspaceFileSnapshot {
        relativePath = KnowledgeIngestionPolicy.normalizeAndValidatePath(relativePath);
        content = KnowledgeIngestionPolicy.validateAndNormalizeContent(content);
        if (content.length() > MAX_FILE_CHARS) {
            throw new DomainException(
                    ErrorCode.VALIDATION_ERROR,
                    "Arquivo de contexto do workspace excede o limite de " + MAX_FILE_CHARS + " caracteres."
            );
        }
    }
}
