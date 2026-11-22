package io.github.jvpgjava.creatingj.agent.application.port.out;

import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;
import java.util.Optional;

public interface AgentRunStorePort {

    AgentRun save(AgentRun run);

    Optional<AgentRun> findById(AgentRunId runId);

    List<AgentRun> findRecentByProjectAndOwner(ProjectId projectId, UserId ownerUserId, int limit);
}
