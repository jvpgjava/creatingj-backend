package io.github.jvpgjava.creatingj.knowledge.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface ListKnowledgeIndexJobsUseCase {

    List<KnowledgeIndexJob> execute(Query query);

    record Query(UserId actorUserId, ProjectId projectId, int limit) {
    }
}
