package io.github.jvpgjava.creatingj.task.domain;

import io.github.jvpgjava.creatingj.agent.domain.AgentToolName;
import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Objects;

public record ToolApproval(
        ToolApprovalId id,
        TaskId taskId,
        UserId ownerUserId,
        DeviceId deviceId,
        AgentToolName toolName,
        String targetPath,
        String commandText,
        String diffPreview,
        RemoteApprovalStatus status,
        Instant expiresAt,
        Instant decidedAt,
        Instant createdAt
) {

    public ToolApproval {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public boolean belongsTo(TaskId candidateTaskId, UserId candidateOwner) {
        return taskId.equals(candidateTaskId) && ownerUserId.equals(candidateOwner);
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public ToolApproval decide(ToolDecisionAction action, Instant now) {
        Objects.requireNonNull(action, "action");
        if (status != RemoteApprovalStatus.PENDING) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Aprovacao ja decidida ou expirada.");
        }
        if (isExpiredAt(now)) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Aprovacao pontual expirada.");
        }
        RemoteApprovalStatus next = action == ToolDecisionAction.APPROVE
                ? RemoteApprovalStatus.APPROVED
                : RemoteApprovalStatus.REJECTED;
        return new ToolApproval(
                id, taskId, ownerUserId, deviceId, toolName, targetPath, commandText, diffPreview,
                next, expiresAt, now, createdAt
        );
    }

    public ToolApproval expireOrRejectIfPending(RemoteApprovalStatus terminalStatus, Instant now) {
        if (status != RemoteApprovalStatus.PENDING) {
            return this;
        }
        return new ToolApproval(
                id, taskId, ownerUserId, deviceId, toolName, targetPath, commandText, diffPreview,
                terminalStatus, expiresAt, now, createdAt
        );
    }
}
