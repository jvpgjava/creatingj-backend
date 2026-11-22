package io.github.jvpgjava.creatingj.project.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.in.GetProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;

public final class GetProjectInteractor implements GetProjectUseCase {

    private final ProjectStorePort projectStore;

    public GetProjectInteractor(ProjectStorePort projectStore) {
        this.projectStore = projectStore;
    }

    @Override
    public Project execute(Query query) {
        if (query == null || query.actorUserId() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        if (query.projectId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do projeto obrigatorio.");
        }
        Project project = projectStore.findById(query.projectId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado."));
        if (!project.isOwnedBy(query.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado.");
        }
        return project;
    }
}
