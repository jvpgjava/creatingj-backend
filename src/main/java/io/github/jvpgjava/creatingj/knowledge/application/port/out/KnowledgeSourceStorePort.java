package io.github.jvpgjava.creatingj.knowledge.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSourceStorePort {

    KnowledgeSource save(KnowledgeSource source);

    Optional<KnowledgeSource> findById(KnowledgeSourceId sourceId);

    Optional<KnowledgeSource> findByProjectAndPath(ProjectId projectId, String sourcePath);

    List<KnowledgeSource> findByProjectAndOwner(ProjectId projectId, UserId ownerUserId);

    void deleteById(KnowledgeSourceId sourceId);

    void deleteByProject(ProjectId projectId, UserId ownerUserId);
}
