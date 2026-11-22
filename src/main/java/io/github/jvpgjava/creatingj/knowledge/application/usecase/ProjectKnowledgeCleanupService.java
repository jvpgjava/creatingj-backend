package io.github.jvpgjava.creatingj.knowledge.application.usecase;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectKnowledgeCleanupPort;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public final class ProjectKnowledgeCleanupService implements ProjectKnowledgeCleanupPort {

    private final VectorKnowledgePort vectorStore;
    private final KnowledgeSourceStorePort sourceStore;

    public ProjectKnowledgeCleanupService(VectorKnowledgePort vectorStore, KnowledgeSourceStorePort sourceStore) {
        this.vectorStore = vectorStore;
        this.sourceStore = sourceStore;
    }

    @Override
    public void deleteAllForProject(ProjectId projectId, UserId ownerUserId) {
        vectorStore.deleteByProject(projectId, ownerUserId);
        sourceStore.deleteByProject(projectId, ownerUserId);
    }
}
