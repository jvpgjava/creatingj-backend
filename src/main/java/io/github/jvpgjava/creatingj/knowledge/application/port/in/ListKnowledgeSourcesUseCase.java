package io.github.jvpgjava.creatingj.knowledge.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface ListKnowledgeSourcesUseCase {

    List<KnowledgeSource> execute(Query query);

    record Query(UserId actorUserId, ProjectId projectId) {
    }
}
