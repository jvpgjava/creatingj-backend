package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Instant;

public record AgentToolProposal(
        ToolProposalId id,
        AgentRunId runId,
        ProjectId projectId,
        UserId ownerUserId,
        int stepIndex,
        AgentToolName toolName,
        String targetPath,
        String commandText,
        String rationale,
        ToolPolicyDecision policyDecision,
        String policyReason,
        ToolApprovalStatus approvalStatus,
        Instant decidedAt,
        Instant createdAt
) {

    public AgentToolProposal {
        if (id == null || runId == null || projectId == null || ownerUserId == null || toolName == null
                || policyDecision == null || approvalStatus == null || createdAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de proposta ausentes.");
        }
        if (stepIndex < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Indice de passo invalido.");
        }
        rationale = (rationale == null || rationale.isBlank())
                ? "Sem justificativa informada."
                : truncate(rationale, 500);
        policyReason = (policyReason == null || policyReason.isBlank())
                ? "Avaliado por politica."
                : truncate(policyReason, 240);
        if (targetPath != null) {
            targetPath = truncate(targetPath.trim(), 260);
        }
        if (commandText != null) {
            commandText = truncate(commandText.trim(), 1000);
        }
    }

    public boolean belongsTo(AgentRunId candidateRunId, ProjectId candidateProjectId, UserId candidateOwnerUserId) {
        return runId.equals(candidateRunId)
                && projectId.equals(candidateProjectId)
                && ownerUserId.equals(candidateOwnerUserId);
    }

    public AgentToolProposal applyDecision(ToolDecisionAction action, Instant now) {
        if (action == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Acao de decisao obrigatoria.");
        }
        if (policyDecision == ToolPolicyDecision.DENIED_BY_POLICY) {
            throw new DomainException(
                    ErrorCode.INVALID_STATE,
                    "Proposta negada por politica de seguranca nao pode ser aprovada."
            );
        }
        if (approvalStatus != ToolApprovalStatus.PENDING) {
            throw new DomainException(
                    ErrorCode.INVALID_STATE,
                    "Apenas propostas pendentes podem receber decisao."
            );
        }
        ToolApprovalStatus nextStatus = action == ToolDecisionAction.APPROVE
                ? ToolApprovalStatus.APPROVED
                : ToolApprovalStatus.REJECTED;
        return new AgentToolProposal(
                id, runId, projectId, ownerUserId, stepIndex, toolName, targetPath, commandText,
                rationale, policyDecision, policyReason, nextStatus, now, createdAt
        );
    }

    public AgentToolProposal cancelIfPending(Instant now) {
        if (approvalStatus != ToolApprovalStatus.PENDING) {
            return this;
        }
        return new AgentToolProposal(
                id, runId, projectId, ownerUserId, stepIndex, toolName, targetPath, commandText,
                rationale, policyDecision, policyReason, ToolApprovalStatus.REJECTED, now, createdAt
        );
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
