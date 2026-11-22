package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Instant;
import java.util.List;

public record AgentRun(
        AgentRunId id,
        ProjectId projectId,
        UserId ownerUserId,
        String goal,
        String modelId,
        String planSummary,
        AgentRunStatus status,
        int workspaceFileCount,
        int ragMatchCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static final int MAX_GOAL_LENGTH = 2000;

    public AgentRun {
        if (id == null || projectId == null || ownerUserId == null || status == null
                || createdAt == null || updatedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de execucao ausentes.");
        }
        if (goal == null || goal.isBlank() || goal.trim().length() > MAX_GOAL_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Objetivo do agente invalido.");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Modelo obrigatorio.");
        }
        if (planSummary == null || planSummary.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Resumo do plano obrigatorio.");
        }
        if (workspaceFileCount < 0 || ragMatchCount < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Contagens de contexto invalidas.");
        }
        goal = goal.trim();
        modelId = modelId.trim();
        planSummary = planSummary.trim();
    }

    public boolean belongsTo(ProjectId candidateProjectId, UserId candidateOwnerUserId) {
        return projectId.equals(candidateProjectId) && ownerUserId.equals(candidateOwnerUserId);
    }

    public AgentRun withStatus(AgentRunStatus nextStatus, Instant now) {
        return new AgentRun(
                id, projectId, ownerUserId, goal, modelId, planSummary, nextStatus,
                workspaceFileCount, ragMatchCount, createdAt, now
        );
    }

    public static AgentRunStatus deriveStatus(List<AgentToolProposal> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return AgentRunStatus.COMPLETED;
        }
        boolean hasPending = proposals.stream().anyMatch(p -> p.approvalStatus() == ToolApprovalStatus.PENDING);
        if (hasPending) {
            return AgentRunStatus.AWAITING_APPROVAL;
        }
        boolean hasExecutable = proposals.stream().anyMatch(p ->
                p.approvalStatus() == ToolApprovalStatus.AUTO_APPROVED
                        || p.approvalStatus() == ToolApprovalStatus.APPROVED);
        return hasExecutable ? AgentRunStatus.READY_FOR_CLIENT : AgentRunStatus.DENIED;
    }
}
