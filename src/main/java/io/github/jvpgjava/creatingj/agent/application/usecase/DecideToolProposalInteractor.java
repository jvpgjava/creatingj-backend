package io.github.jvpgjava.creatingj.agent.application.usecase;

import io.github.jvpgjava.creatingj.agent.application.port.in.DecideToolProposalUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentToolProposalStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunStatus;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DecideToolProposalInteractor implements DecideToolProposalUseCase {

    private final ProjectStorePort projectStore;
    private final AgentRunStorePort runStore;
    private final AgentToolProposalStorePort proposalStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public DecideToolProposalInteractor(
            ProjectStorePort projectStore,
            AgentRunStorePort runStore,
            AgentToolProposalStorePort proposalStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.projectStore = projectStore;
        this.runStore = runStore;
        this.proposalStore = proposalStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        if (command == null || command.runId() == null || command.proposalId() == null) {
            throw new DomainException(
                    ErrorCode.VALIDATION_ERROR,
                    "Identificadores de execucao e proposta obrigatorios."
            );
        }
        AgentProjectGuard.requireOwnedProject(projectStore, command.actorUserId(), command.projectId());
        AgentRun run = runStore.findById(command.runId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Execucao do agente nao encontrada."));
        if (!run.belongsTo(command.projectId(), command.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Execucao do agente nao encontrada.");
        }
        if (run.status() == AgentRunStatus.CANCELLED) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Execucao cancelada nao aceita novas decisoes.");
        }
        AgentToolProposal proposal = proposalStore.findById(command.proposalId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Proposta nao encontrada."));
        if (!proposal.belongsTo(run.id(), command.projectId(), command.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Proposta nao encontrada.");
        }

        Instant now = clock.now();
        AgentToolProposal updatedProposal = proposalStore.save(proposal.applyDecision(command.action(), now));
        List<AgentToolProposal> allProposals = proposalStore.findByRunOrderByStepAsc(run.id());
        AgentRun updatedRun = runStore.save(run.withStatus(AgentRun.deriveStatus(allProposals), now));

        AuditAction auditAction = command.action() == ToolDecisionAction.APPROVE
                ? AuditAction.AGENT_TOOL_APPROVED
                : AuditAction.AGENT_TOOL_REJECTED;
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                auditAction,
                "SUCCESS",
                "projectId=" + command.projectId() + " runId=" + run.id()
                        + " proposalId=" + updatedProposal.id() + " tool=" + updatedProposal.toolName(),
                now
        ));
        return new Result(updatedRun, updatedProposal);
    }
}
