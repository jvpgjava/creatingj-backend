package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Instant;

public record KnowledgeSource(
        KnowledgeSourceId id,
        ProjectId projectId,
        UserId ownerUserId,
        String sourcePath,
        String title,
        String contentHash,
        String embeddingVersion,
        KnowledgeSourceStatus status,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {

    private static final int MAX_TITLE_LENGTH = 160;

    public KnowledgeSource {
        if (id == null || projectId == null || ownerUserId == null || status == null
                || createdAt == null || updatedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de fonte ausentes.");
        }
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho da fonte obrigatorio.");
        }
        if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Titulo da fonte invalido.");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Hash de conteudo obrigatorio.");
        }
        if (embeddingVersion == null || embeddingVersion.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Versao de embedding obrigatoria.");
        }
        if (chunkCount < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Contagem de chunks invalida.");
        }
        title = title.trim();
        embeddingVersion = embeddingVersion.trim();
    }

    public boolean isUnchanged(String candidateHash, String candidateEmbeddingVersion) {
        return status == KnowledgeSourceStatus.INDEXED
                && contentHash.equals(candidateHash)
                && embeddingVersion.equals(candidateEmbeddingVersion);
    }

    public boolean belongsTo(ProjectId candidateProjectId, UserId candidateOwnerUserId) {
        return projectId.equals(candidateProjectId) && ownerUserId.equals(candidateOwnerUserId);
    }
}
