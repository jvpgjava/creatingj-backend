package io.github.jvpgjava.creatingj.project.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.project.application.port.in.DeleteProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectKnowledgeCleanupPort;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;

import java.util.UUID;

public final class DeleteProjectInteractor implements DeleteProjectUseCase {

    private final ProjectStorePort projectStore;
    private final ProjectKnowledgeCleanupPort knowledgeCleanup;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public DeleteProjectInteractor(
            ProjectStorePort projectStore,
            ProjectKnowledgeCleanupPort knowledgeCleanup,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.projectStore = projectStore;
        this.knowledgeCleanup = knowledgeCleanup;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public void execute(Command command) {
        if (command == null || command.actorUserId() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        if (command.projectId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do projeto obrigatorio.");
        }
        Project project = projectStore.findById(command.projectId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado."));
        if (!project.isOwnedBy(command.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado.");
        }
        knowledgeCleanup.deleteAllForProject(project.id(), command.actorUserId());
        projectStore.deleteById(project.id());
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.PROJECT_DELETED,
                "SUCCESS",
                "projectId=" + project.id(),
                clock.now()
        ));
    }
}
