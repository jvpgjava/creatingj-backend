package io.github.jvpgjava.creatingj.agent.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunDetailResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunSummaryResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunsResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentToolProposalResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.CreateAgentRunRequest;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.ToolProposalDecisionResponse;
import io.github.jvpgjava.creatingj.agent.application.port.in.CreateAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.DecideToolProposalUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.GetAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.agent.domain.WorkspaceContextBundle;
import io.github.jvpgjava.creatingj.agent.domain.WorkspaceFileSnapshot;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public final class AgentHttpMapper {

    public CreateAgentRunUseCase.Command toCreateCommand(
            UserId actorUserId,
            String projectId,
            CreateAgentRunRequest request
    ) {
        List<WorkspaceFileSnapshot> snapshots = request.workspaceFiles() == null
                ? List.of()
                : request.workspaceFiles().stream()
                        .map(f -> new WorkspaceFileSnapshot(f.relativePath(), f.content()))
                        .toList();
        boolean includeRag = Boolean.TRUE.equals(request.includeProjectRag());
        int ragTopK = request.ragTopK() == null ? 4 : request.ragTopK();
        return new CreateAgentRunUseCase.Command(
                actorUserId,
                ProjectId.parse(projectId),
                request.goal(),
                request.modelId(),
                new WorkspaceContextBundle(snapshots),
                includeRag,
                ragTopK
        );
    }

    public AgentRunSummaryResponse toSummary(AgentRun run) {
        return new AgentRunSummaryResponse(
                run.id().toString(),
                run.projectId().toString(),
                run.goal(),
                run.modelId(),
                run.planSummary(),
                run.status().name(),
                run.workspaceFileCount(),
                run.ragMatchCount(),
                run.createdAt(),
                run.updatedAt()
        );
    }

    public AgentToolProposalResponse toProposal(AgentToolProposal proposal) {
        return new AgentToolProposalResponse(
                proposal.id().toString(),
                proposal.stepIndex(),
                proposal.toolName().name(),
                proposal.targetPath(),
                proposal.commandText(),
                proposal.rationale(),
                proposal.policyDecision().name(),
                proposal.policyReason(),
                proposal.approvalStatus().name(),
                proposal.decidedAt(),
                proposal.createdAt()
        );
    }

    public AgentRunDetailResponse toDetail(CreateAgentRunUseCase.Result result) {
        return new AgentRunDetailResponse(
                toSummary(result.run()),
                result.proposals().stream().map(this::toProposal).toList()
        );
    }

    public AgentRunDetailResponse toDetail(GetAgentRunUseCase.Result result) {
        return new AgentRunDetailResponse(
                toSummary(result.run()),
                result.proposals().stream().map(this::toProposal).toList()
        );
    }

    public AgentRunsResponse toRuns(List<AgentRun> runs) {
        return new AgentRunsResponse(runs.stream().map(this::toSummary).toList());
    }

    public ToolProposalDecisionResponse toDecision(DecideToolProposalUseCase.Result result) {
        return new ToolProposalDecisionResponse(toSummary(result.run()), toProposal(result.proposal()));
    }
}
