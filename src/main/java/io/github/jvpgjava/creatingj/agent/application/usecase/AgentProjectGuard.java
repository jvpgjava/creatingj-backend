package io.github.jvpgjava.creatingj.agent.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

final class AgentProjectGuard {

    private AgentProjectGuard() {
    }

    static Project requireOwnedProject(ProjectStorePort projectStore, UserId actorUserId, ProjectId projectId) {
        if (actorUserId == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        if (projectId == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do projeto obrigatorio.");
        }
        Project project = projectStore.findById(projectId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado."));
        if (!project.isOwnedBy(actorUserId)) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado.");
        }
        return project;
    }
}
