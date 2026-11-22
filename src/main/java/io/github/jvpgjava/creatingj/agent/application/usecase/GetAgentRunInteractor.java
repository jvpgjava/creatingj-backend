package io.github.jvpgjava.creatingj.agent.application.usecase;

import io.github.jvpgjava.creatingj.agent.application.port.in.GetAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentToolProposalStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.util.List;

public final class GetAgentRunInteractor implements GetAgentRunUseCase {

    private final ProjectStorePort projectStore;
    private final AgentRunStorePort runStore;
    private final AgentToolProposalStorePort proposalStore;

    public GetAgentRunInteractor(
            ProjectStorePort projectStore,
            AgentRunStorePort runStore,
            AgentToolProposalStorePort proposalStore
    ) {
        this.projectStore = projectStore;
        this.runStore = runStore;
        this.proposalStore = proposalStore;
    }

    @Override
    public Result execute(Query query) {
        if (query == null || query.runId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador da execucao obrigatorio.");
        }
        AgentProjectGuard.requireOwnedProject(projectStore, query.actorUserId(), query.projectId());
        AgentRun run = runStore.findById(query.runId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Execucao do agente nao encontrada."));
        if (!run.belongsTo(query.projectId(), query.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Execucao do agente nao encontrada.");
        }
        List<AgentToolProposal> proposals = proposalStore.findByRunOrderByStepAsc(run.id());
        return new Result(run, proposals);
    }
}
