package io.github.jvpgjava.creatingj.project.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.in.CreateProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.time.Instant;
import java.util.UUID;

public final class CreateProjectInteractor implements CreateProjectUseCase {

    private final ProjectStorePort projectStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public CreateProjectInteractor(ProjectStorePort projectStore, AuditPort auditPort, ClockPort clock) {
        this.projectStore = projectStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Project execute(Command command) {
        if (command == null || command.actorUserId() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        Instant now = clock.now();
        Project project = Project.create(
                ProjectId.random(),
                command.actorUserId(),
                command.name(),
                command.slug(),
                command.description(),
                now
        );
        if (projectStore.findByOwnerAndSlug(command.actorUserId(), project.slug()).isPresent()) {
            throw new DomainException(ErrorCode.CONFLICT, "Ja existe um projeto com este slug para a conta.");
        }
        Project saved = projectStore.save(project);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.PROJECT_CREATED,
                "SUCCESS",
                "projectId=" + saved.id() + " slug=" + saved.slug(),
                now
        ));
        return saved;
    }
}
