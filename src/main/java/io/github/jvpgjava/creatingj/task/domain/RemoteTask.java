package io.github.jvpgjava.creatingj.task.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record RemoteTask(
        TaskId id,
        UserId ownerUserId,
        ProjectId projectId,
        DeviceId deviceId,
        String idempotencyKey,
        String title,
        String instruction,
        TaskStatus status,
        Instant leaseExpiresAt,
        String resultSummary,
        String errorCode,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    public static final Duration LEASE_DURATION = Duration.ofSeconds(60);

    public RemoteTask {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (title == null || title.isBlank() || title.trim().length() > 160) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Titulo da tarefa invalido.");
        }
        if (instruction == null || instruction.isBlank() || instruction.trim().length() > 4000) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Instrucao da tarefa invalida.");
        }
        title = title.trim();
        instruction = instruction.trim();
        if (idempotencyKey != null && idempotencyKey.isBlank()) {
            idempotencyKey = null;
        }
    }

    public boolean isOwnedBy(UserId candidate) {
        return ownerUserId.equals(candidate);
    }

    public boolean isAssignedTo(DeviceId candidate) {
        return deviceId.equals(candidate);
    }

    public boolean isLeasableAt(Instant now) {
        return status == TaskStatus.QUEUED
                || ((status == TaskStatus.LEASED || status == TaskStatus.RUNNING)
                        && leaseExpiresAt != null && !leaseExpiresAt.isAfter(now));
    }

    public RemoteTask lease(Instant now) {
        return new RemoteTask(
                id, ownerUserId, projectId, deviceId, idempotencyKey, title, instruction,
                TaskStatus.LEASED, now.plus(LEASE_DURATION), resultSummary, errorCode, createdAt, now, completedAt
        );
    }

    public RemoteTask markRunning(Instant now) {
        return new RemoteTask(
                id, ownerUserId, projectId, deviceId, idempotencyKey, title, instruction,
                TaskStatus.RUNNING, now.plus(LEASE_DURATION), resultSummary, errorCode, createdAt, now, completedAt
        );
    }

    public RemoteTask markAwaitingApproval(Instant now) {
        return new RemoteTask(
                id, ownerUserId, projectId, deviceId, idempotencyKey, title, instruction,
                TaskStatus.AWAITING_APPROVAL, now.plus(LEASE_DURATION), resultSummary, errorCode,
                createdAt, now, completedAt
        );
    }

    public RemoteTask complete(boolean succeeded, String summary, String failureCode, Instant now) {
        if (status.isTerminal()) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Tarefa ja finalizada.");
        }
        TaskStatus next = succeeded ? TaskStatus.COMPLETED : TaskStatus.FAILED;
        return new RemoteTask(
                id, ownerUserId, projectId, deviceId, idempotencyKey, title, instruction,
                next, null, summary, succeeded ? null : failureCode, createdAt, now, now
        );
    }

    public RemoteTask cancel(Instant now) {
        if (status.isTerminal()) {
            return this;
        }
        return new RemoteTask(
                id, ownerUserId, projectId, deviceId, idempotencyKey, title, instruction,
                TaskStatus.CANCELLED, null, resultSummary, errorCode, createdAt, now, now
        );
    }
}
