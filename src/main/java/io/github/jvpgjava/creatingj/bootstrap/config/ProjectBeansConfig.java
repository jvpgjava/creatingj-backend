package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.project.adapter.in.web.mapper.v1.ProjectHttpMapper;
import io.github.jvpgjava.creatingj.project.application.port.in.CreateProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.DeleteProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.GetProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.ListAuthorizedProjectsUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectKnowledgeCleanupPort;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.application.usecase.CreateProjectInteractor;
import io.github.jvpgjava.creatingj.project.application.usecase.DeleteProjectInteractor;
import io.github.jvpgjava.creatingj.project.application.usecase.GetProjectInteractor;
import io.github.jvpgjava.creatingj.project.application.usecase.ListAuthorizedProjectsInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectBeansConfig {

    @Bean
    public CreateProjectUseCase createProjectUseCase(
            ProjectStorePort projectStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new CreateProjectInteractor(projectStore, auditPort, clock);
    }

    @Bean
    public ListAuthorizedProjectsUseCase listAuthorizedProjectsUseCase(ProjectStorePort projectStore) {
        return new ListAuthorizedProjectsInteractor(projectStore);
    }

    @Bean
    public GetProjectUseCase getProjectUseCase(ProjectStorePort projectStore) {
        return new GetProjectInteractor(projectStore);
    }

    @Bean
    public DeleteProjectUseCase deleteProjectUseCase(
            ProjectStorePort projectStore,
            ProjectKnowledgeCleanupPort knowledgeCleanup,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new DeleteProjectInteractor(projectStore, knowledgeCleanup, auditPort, clock);
    }

    @Bean
    public ProjectHttpMapper projectHttpMapper() {
        return new ProjectHttpMapper();
    }
}
