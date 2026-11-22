package io.github.jvpgjava.creatingj.project.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.project.adapter.in.web.apidocs.v1.ProjectApiDocs;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.CreateProjectRequest;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectResponse;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectsPageResponse;
import io.github.jvpgjava.creatingj.project.adapter.in.web.mapper.v1.ProjectHttpMapper;
import io.github.jvpgjava.creatingj.project.application.port.in.CreateProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.DeleteProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.GetProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.ListAuthorizedProjectsUseCase;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public final class ProjectController implements ProjectApiDocs {

    private final CreateProjectUseCase createProject;
    private final ListAuthorizedProjectsUseCase listProjects;
    private final GetProjectUseCase getProject;
    private final DeleteProjectUseCase deleteProject;
    private final ProjectHttpMapper mapper;

    public ProjectController(
            CreateProjectUseCase createProject,
            ListAuthorizedProjectsUseCase listProjects,
            GetProjectUseCase getProject,
            DeleteProjectUseCase deleteProject,
            ProjectHttpMapper mapper
    ) {
        this.createProject = createProject;
        this.listProjects = listProjects;
        this.getProject = getProject;
        this.deleteProject = deleteProject;
        this.mapper = mapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var created = createProject.execute(mapper.toCreateCommand(actor.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @Override
    @GetMapping
    public ResponseEntity<ProjectsPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = listProjects.execute(new ListAuthorizedProjectsUseCase.Query(actor.id(), page, size));
        return ResponseEntity.ok(mapper.toPage(result));
    }

    @Override
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getById(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var project = getProject.execute(new GetProjectUseCase.Query(actor.id(), ProjectId.parse(projectId)));
        return ResponseEntity.ok(mapper.toResponse(project));
    }

    @Override
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        deleteProject.execute(new DeleteProjectUseCase.Command(actor.id(), ProjectId.parse(projectId)));
        return ResponseEntity.noContent().build();
    }
}
