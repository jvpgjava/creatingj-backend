package io.github.jvpgjava.creatingj.agent.application.port.in;

import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.agent.domain.WorkspaceContextBundle;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public interface CreateAgentRunUseCase {

    Result execute(Command command);

    record Command(
            UserId actorUserId,
            ProjectId projectId,
            String goal,
            String modelId,
            WorkspaceContextBundle workspaceContext,
            boolean includeProjectRag,
            int ragTopK
    ) {
    }

    record Result(AgentRun run, List<AgentToolProposal> proposals) {
    }
}
