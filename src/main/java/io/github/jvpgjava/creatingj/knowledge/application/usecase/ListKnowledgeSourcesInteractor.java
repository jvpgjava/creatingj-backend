package io.github.jvpgjava.creatingj.knowledge.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeSourcesUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;

import java.util.List;

public final class ListKnowledgeSourcesInteractor implements ListKnowledgeSourcesUseCase {

    private final ProjectStorePort projectStore;
    private final KnowledgeSourceStorePort sourceStore;

    public ListKnowledgeSourcesInteractor(ProjectStorePort projectStore, KnowledgeSourceStorePort sourceStore) {
        this.projectStore = projectStore;
        this.sourceStore = sourceStore;
    }

    @Override
    public List<KnowledgeSource> execute(Query query) {
        if (query == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Consulta de fontes obrigatoria.");
        }
        ProjectAuthorizationGuard.requireOwnedProject(projectStore, query.actorUserId(), query.projectId());
        return sourceStore.findByProjectAndOwner(query.projectId(), query.actorUserId());
    }
}
