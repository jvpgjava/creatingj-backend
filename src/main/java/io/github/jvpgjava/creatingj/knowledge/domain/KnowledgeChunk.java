package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Instant;

public record KnowledgeChunk(
        KnowledgeChunkId id,
        KnowledgeSourceId sourceId,
        ProjectId projectId,
        UserId ownerUserId,
        int chunkIndex,
        String content,
        String contentHash,
        int tokenEstimate,
        String embeddingVersion,
        EmbeddingVector embedding,
        Instant createdAt
) {

    public KnowledgeChunk {
        if (id == null || sourceId == null || projectId == null || ownerUserId == null
                || embedding == null || createdAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios do chunk ausentes.");
        }
        if (chunkIndex < 0 || tokenEstimate < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Indices ou estimativa do chunk invalidos.");
        }
        if (content == null || content.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo do chunk obrigatorio.");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Hash do chunk obrigatorio.");
        }
        if (embeddingVersion == null || embeddingVersion.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Versao de embedding do chunk obrigatoria.");
        }
    }
}
