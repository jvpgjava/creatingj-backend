package io.github.jvpgjava.creatingj.agent.application.usecase;

import io.github.jvpgjava.creatingj.agent.application.port.in.ListAgentRunsUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.util.List;

public final class ListAgentRunsInteractor implements ListAgentRunsUseCase {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ProjectStorePort projectStore;
    private final AgentRunStorePort runStore;

    public ListAgentRunsInteractor(ProjectStorePort projectStore, AgentRunStorePort runStore) {
        this.projectStore = projectStore;
        this.runStore = runStore;
    }

    @Override
    public List<AgentRun> execute(Query query) {
        if (query == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Consulta de execucoes obrigatoria.");
        }
        AgentProjectGuard.requireOwnedProject(projectStore, query.actorUserId(), query.projectId());
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        return runStore.findRecentByProjectAndOwner(query.projectId(), query.actorUserId(), limit);
    }
}
