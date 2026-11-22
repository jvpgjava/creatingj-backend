package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Instant;

public record KnowledgeIndexJob(
        KnowledgeIndexJobId id,
        ProjectId projectId,
        UserId ownerUserId,
        KnowledgeSourceId sourceId,
        String sourcePath,
        KnowledgeJobOperation operation,
        KnowledgeJobStatus status,
        int chunksIndexed,
        String errorCode,
        Instant createdAt,
        Instant completedAt
) {

    public KnowledgeIndexJob {
        if (id == null || projectId == null || ownerUserId == null || operation == null
                || status == null || createdAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de job ausentes.");
        }
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho do job obrigatorio.");
        }
        if (chunksIndexed < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Contagem de chunks do job invalida.");
        }
    }

    public static KnowledgeIndexJob pending(
            ProjectId projectId,
            UserId ownerUserId,
            KnowledgeSourceId sourceId,
            String sourcePath,
            KnowledgeJobOperation operation,
            Instant now
    ) {
        return new KnowledgeIndexJob(
                KnowledgeIndexJobId.random(),
                projectId,
                ownerUserId,
                sourceId,
                sourcePath,
                operation,
                KnowledgeJobStatus.RUNNING,
                0,
                null,
                now,
                null
        );
    }

    public KnowledgeIndexJob complete(KnowledgeSourceId resolvedSourceId, int indexedCount, Instant finishedAt) {
        return new KnowledgeIndexJob(
                id,
                projectId,
                ownerUserId,
                resolvedSourceId,
                sourcePath,
                operation,
                KnowledgeJobStatus.COMPLETED,
                indexedCount,
                null,
                createdAt,
                finishedAt
        );
    }

    public KnowledgeIndexJob skippedUnchanged(KnowledgeSourceId resolvedSourceId, Instant finishedAt) {
        return new KnowledgeIndexJob(
                id,
                projectId,
                ownerUserId,
                resolvedSourceId,
                sourcePath,
                operation,
                KnowledgeJobStatus.SKIPPED_UNCHANGED,
                0,
                null,
                createdAt,
                finishedAt
        );
    }

    public KnowledgeIndexJob fail(String failureCode, Instant finishedAt) {
        return new KnowledgeIndexJob(
                id,
                projectId,
                ownerUserId,
                sourceId,
                sourcePath,
                operation,
                KnowledgeJobStatus.FAILED,
                0,
                failureCode,
                createdAt,
                finishedAt
        );
    }
}
