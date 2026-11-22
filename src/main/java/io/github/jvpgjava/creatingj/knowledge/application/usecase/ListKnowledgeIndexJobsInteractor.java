package io.github.jvpgjava.creatingj.knowledge.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeIndexJobsUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeIndexJobStorePort;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.util.List;

public final class ListKnowledgeIndexJobsInteractor implements ListKnowledgeIndexJobsUseCase {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ProjectStorePort projectStore;
    private final KnowledgeIndexJobStorePort jobStore;

    public ListKnowledgeIndexJobsInteractor(ProjectStorePort projectStore, KnowledgeIndexJobStorePort jobStore) {
        this.projectStore = projectStore;
        this.jobStore = jobStore;
    }

    @Override
    public List<KnowledgeIndexJob> execute(Query query) {
        if (query == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Consulta de jobs obrigatoria.");
        }
        ProjectAuthorizationGuard.requireOwnedProject(projectStore, query.actorUserId(), query.projectId());
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        return jobStore.findRecentByProjectAndOwner(query.projectId(), query.actorUserId(), limit);
    }
}
