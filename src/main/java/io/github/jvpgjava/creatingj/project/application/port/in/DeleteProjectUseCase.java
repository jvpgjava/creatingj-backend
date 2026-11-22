package io.github.jvpgjava.creatingj.project.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public interface DeleteProjectUseCase {

    void execute(Command command);

    record Command(UserId actorUserId, ProjectId projectId) {
    }
}
