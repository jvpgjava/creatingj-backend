package io.github.jvpgjava.creatingj.project.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.Project;

public interface CreateProjectUseCase {

    Project execute(Command command);

    record Command(UserId actorUserId, String name, String slug, String description) {
    }
}
