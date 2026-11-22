package io.github.jvpgjava.creatingj.agent.application.usecase;

import io.github.jvpgjava.creatingj.agent.application.port.in.CancelAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentToolProposalStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunStatus;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
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

public final class CancelAgentRunInteractor implements CancelAgentRunUseCase {

    private final ProjectStorePort projectStore;
    private final AgentRunStorePort runStore;
    private final AgentToolProposalStorePort proposalStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public CancelAgentRunInteractor(
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
    public AgentRun execute(Command command) {
        if (command == null || command.runId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da execucao obrigatorio.");
        }
        AgentProjectGuard.requireOwnedProject(projectStore, command.actorUserId(), command.projectId());
        AgentRun run = runStore.findById(command.runId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Execucao do agente nao encontrada."));
        if (!run.belongsTo(command.projectId(), command.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Execucao do agente nao encontrada.");
        }
        if (run.status() == AgentRunStatus.CANCELLED) {
            return run;
        }
        Instant now = clock.now();
        List<AgentToolProposal> updatedProposals = proposalStore.findByRunOrderByStepAsc(run.id()).stream()
                .map(p -> p.cancelIfPending(now))
                .toList();
        proposalStore.saveAll(updatedProposals);
        AgentRun cancelled = runStore.save(run.withStatus(AgentRunStatus.CANCELLED, now));
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.AGENT_RUN_CANCELLED,
                "SUCCESS",
                "projectId=" + command.projectId() + " runId=" + run.id(),
                now
        ));
        return cancelled;
    }
}
