package io.github.jvpgjava.creatingj.agent.application.port.in;

import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface GetAgentRunUseCase {

    Result execute(Query query);

    record Query(UserId actorUserId, ProjectId projectId, AgentRunId runId) {
    }

    record Result(AgentRun run, List<AgentToolProposal> proposals) {
    }
}
