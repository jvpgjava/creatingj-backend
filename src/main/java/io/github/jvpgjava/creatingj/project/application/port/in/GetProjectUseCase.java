package io.github.jvpgjava.creatingj.project.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public interface GetProjectUseCase {

    Project execute(Query query);

    record Query(UserId actorUserId, ProjectId projectId) {
    }
}
