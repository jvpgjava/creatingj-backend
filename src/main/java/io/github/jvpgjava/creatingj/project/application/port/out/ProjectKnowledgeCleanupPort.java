package io.github.jvpgjava.creatingj.project.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public interface ProjectKnowledgeCleanupPort {

    void deleteAllForProject(ProjectId projectId, UserId ownerUserId);
}
