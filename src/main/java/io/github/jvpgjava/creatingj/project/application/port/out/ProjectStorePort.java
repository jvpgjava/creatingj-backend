package io.github.jvpgjava.creatingj.project.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;
import java.util.Optional;

public interface ProjectStorePort {

    Project save(Project project);

    Optional<Project> findById(ProjectId projectId);

    Optional<Project> findByOwnerAndSlug(UserId ownerUserId, String slug);

    List<Project> findByOwner(UserId ownerUserId, int offset, int limit);

    long countByOwner(UserId ownerUserId);

    void deleteById(ProjectId projectId);
}
