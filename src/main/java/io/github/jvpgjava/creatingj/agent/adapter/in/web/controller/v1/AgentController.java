package io.github.jvpgjava.creatingj.agent.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.agent.adapter.in.web.apidocs.v1.AgentApiDocs;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunDetailResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunSummaryResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.AgentRunsResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.CreateAgentRunRequest;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.ToolProposalDecisionRequest;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1.ToolProposalDecisionResponse;
import io.github.jvpgjava.creatingj.agent.adapter.in.web.mapper.v1.AgentHttpMapper;
import io.github.jvpgjava.creatingj.agent.application.port.in.CancelAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.CreateAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.DecideToolProposalUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.GetAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.ListAgentRunsUseCase;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.agent.domain.ToolProposalId;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/agent/runs")
public final class AgentController implements AgentApiDocs {

    private final CreateAgentRunUseCase createAgentRun;
    private final ListAgentRunsUseCase listAgentRuns;
    private final GetAgentRunUseCase getAgentRun;
    private final DecideToolProposalUseCase decideToolProposal;
    private final CancelAgentRunUseCase cancelAgentRun;
    private final AgentHttpMapper mapper;

    public AgentController(
            CreateAgentRunUseCase createAgentRun,
            ListAgentRunsUseCase listAgentRuns,
            GetAgentRunUseCase getAgentRun,
            DecideToolProposalUseCase decideToolProposal,
            CancelAgentRunUseCase cancelAgentRun,
            AgentHttpMapper mapper
    ) {
        this.createAgentRun = createAgentRun;
        this.listAgentRuns = listAgentRuns;
        this.getAgentRun = getAgentRun;
        this.decideToolProposal = decideToolProposal;
        this.cancelAgentRun = cancelAgentRun;
        this.mapper = mapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<AgentRunDetailResponse> createRun(
            @PathVariable String projectId,
            @Valid @RequestBody CreateAgentRunRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = createAgentRun.execute(mapper.toCreateCommand(actor.id(), projectId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDetail(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<AgentRunsResponse> listRuns(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var runs = listAgentRuns.execute(new ListAgentRunsUseCase.Query(actor.id(), ProjectId.parse(projectId), limit));
        return ResponseEntity.ok(mapper.toRuns(runs));
    }

    @Override
    @GetMapping("/{runId}")
    public ResponseEntity<AgentRunDetailResponse> getRun(
            @PathVariable String projectId,
            @PathVariable String runId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = getAgentRun.execute(new GetAgentRunUseCase.Query(
                actor.id(),
                ProjectId.parse(projectId),
                AgentRunId.parse(runId)
        ));
        return ResponseEntity.ok(mapper.toDetail(result));
    }

    @Override
    @PostMapping("/{runId}/proposals/{proposalId}/decision")
    public ResponseEntity<ToolProposalDecisionResponse> decideProposal(
            @PathVariable String projectId,
            @PathVariable String runId,
            @PathVariable String proposalId,
            @Valid @RequestBody ToolProposalDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = decideToolProposal.execute(new DecideToolProposalUseCase.Command(
                actor.id(),
                ProjectId.parse(projectId),
                AgentRunId.parse(runId),
                ToolProposalId.parse(proposalId),
                request.action()
        ));
        return ResponseEntity.ok(mapper.toDecision(result));
    }

    @Override
    @PostMapping("/{runId}/cancel")
    public ResponseEntity<AgentRunSummaryResponse> cancelRun(
            @PathVariable String projectId,
            @PathVariable String runId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var cancelled = cancelAgentRun.execute(new CancelAgentRunUseCase.Command(
                actor.id(),
                ProjectId.parse(projectId),
                AgentRunId.parse(runId)
        ));
        return ResponseEntity.ok(mapper.toSummary(cancelled));
    }
}
