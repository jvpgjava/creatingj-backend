package io.github.jvpgjava.creatingj.project.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.CreateProjectRequest;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectResponse;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectsPageResponse;
import io.github.jvpgjava.creatingj.project.application.port.in.CreateProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.ListAuthorizedProjectsUseCase;
import io.github.jvpgjava.creatingj.project.domain.Project;

import java.util.List;

public final class ProjectHttpMapper {

    public CreateProjectUseCase.Command toCreateCommand(UserId actorUserId, CreateProjectRequest request) {
        return new CreateProjectUseCase.Command(
                actorUserId,
                request.name(),
                request.slug(),
                request.description()
        );
    }

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.id().toString(),
                project.name(),
                project.slug(),
                project.description(),
                project.createdAt(),
                project.updatedAt()
        );
    }

    public ProjectsPageResponse toPage(ListAuthorizedProjectsUseCase.Result result) {
        List<ProjectResponse> items = result.items().stream().map(this::toResponse).toList();
        return new ProjectsPageResponse(items, result.page(), result.size(), result.totalElements());
    }
}
