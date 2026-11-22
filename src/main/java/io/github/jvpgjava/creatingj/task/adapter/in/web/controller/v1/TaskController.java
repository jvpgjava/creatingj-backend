package io.github.jvpgjava.creatingj.task.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.task.adapter.in.web.apidocs.v1.TaskApiDocs;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.CreateRemoteTaskRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.CreateRemoteTaskResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskDetailResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskSummaryResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTasksResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalDecisionRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalDecisionResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.mapper.v1.TaskHttpMapper;
import io.github.jvpgjava.creatingj.task.adapter.in.web.streaming.SseTaskProgressHub;
import io.github.jvpgjava.creatingj.task.application.port.in.ManageUserRemoteTasksUseCase;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/tasks")
public final class TaskController implements TaskApiDocs {

    private final ManageUserRemoteTasksUseCase manageTasks;
    private final SseTaskProgressHub sseHub;
    private final TaskHttpMapper mapper;

    public TaskController(
            ManageUserRemoteTasksUseCase manageTasks,
            SseTaskProgressHub sseHub,
            TaskHttpMapper mapper
    ) {
        this.manageTasks = manageTasks;
        this.sseHub = sseHub;
        this.mapper = mapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<CreateRemoteTaskResponse> create(
            @Valid @RequestBody CreateRemoteTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = manageTasks.create(mapper.toCreateCommand(actor.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCreateResponse(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<RemoteTasksResponse> list(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return ResponseEntity.ok(mapper.toTasks(manageTasks.list(actor.id(), limit)));
    }

    @Override
    @GetMapping("/{taskId}")
    public ResponseEntity<RemoteTaskDetailResponse> getDetail(
            @PathVariable String taskId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return ResponseEntity.ok(mapper.toDetail(manageTasks.getDetail(actor.id(), TaskId.parse(taskId))));
    }

    @Override
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<RemoteTaskSummaryResponse> cancel(
            @PathVariable String taskId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return ResponseEntity.ok(mapper.toSummary(manageTasks.cancel(actor.id(), TaskId.parse(taskId))));
    }

    @Override
    @PostMapping("/{taskId}/approvals/{approvalId}/decision")
    public ResponseEntity<ToolApprovalDecisionResponse> decideApproval(
            @PathVariable String taskId,
            @PathVariable String approvalId,
            @Valid @RequestBody ToolApprovalDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = manageTasks.decideApproval(new ManageUserRemoteTasksUseCase.DecisionCommand(
                actor.id(),
                TaskId.parse(taskId),
                ToolApprovalId.parse(approvalId),
                request.action()
        ));
        return ResponseEntity.ok(mapper.toDecision(result));
    }

    @Override
    @GetMapping(value = "/{taskId}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @PathVariable String taskId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var detail = manageTasks.getDetail(actor.id(), TaskId.parse(taskId));
        return sseHub.subscribe(detail.task().id(), detail.events(), detail.task().status().isTerminal());
    }
}
