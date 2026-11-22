package io.github.jvpgjava.creatingj.knowledge.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public interface IngestKnowledgeSourceUseCase {

    Result execute(Command command);

    record Command(UserId actorUserId, ProjectId projectId, String sourcePath, String title, String content) {
    }

    record Result(KnowledgeSource source, KnowledgeIndexJob job, boolean deduplicated) {
    }
}
