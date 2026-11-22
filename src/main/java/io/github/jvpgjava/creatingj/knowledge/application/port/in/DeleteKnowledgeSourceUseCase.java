package io.github.jvpgjava.creatingj.knowledge.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public interface DeleteKnowledgeSourceUseCase {

    void execute(Command command);

    record Command(UserId actorUserId, ProjectId projectId, KnowledgeSourceId sourceId) {
    }
}
