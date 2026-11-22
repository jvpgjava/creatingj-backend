package io.github.jvpgjava.creatingj.agent.application.port.in;

import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

public interface CancelAgentRunUseCase {

    AgentRun execute(Command command);

    record Command(UserId actorUserId, ProjectId projectId, AgentRunId runId) {
    }
}
