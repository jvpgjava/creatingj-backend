package io.github.jvpgjava.creatingj.project.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.in.ListAuthorizedProjectsUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;

import java.util.List;

public final class ListAuthorizedProjectsInteractor implements ListAuthorizedProjectsUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProjectStorePort projectStore;

    public ListAuthorizedProjectsInteractor(ProjectStorePort projectStore) {
        this.projectStore = projectStore;
    }

    @Override
    public Result execute(Query query) {
        if (query == null || query.actorUserId() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        int page = Math.max(0, query.page());
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);
        int offset = page * size;
        List<Project> items = projectStore.findByOwner(query.actorUserId(), offset, size);
        long total = projectStore.countByOwner(query.actorUserId());
        return new Result(items, page, size, total);
    }
}
