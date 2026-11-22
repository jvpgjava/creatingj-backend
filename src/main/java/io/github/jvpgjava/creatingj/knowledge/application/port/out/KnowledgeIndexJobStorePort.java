package io.github.jvpgjava.creatingj.knowledge.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface KnowledgeIndexJobStorePort {

    KnowledgeIndexJob save(KnowledgeIndexJob job);

    List<KnowledgeIndexJob> findRecentByProjectAndOwner(ProjectId projectId, UserId ownerUserId, int limit);
}
