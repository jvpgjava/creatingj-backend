package io.github.jvpgjava.creatingj.task.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.security.AuthenticatedDevice;
import io.github.jvpgjava.creatingj.task.adapter.in.web.apidocs.v1.RunnerTaskApiDocs;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskSummaryResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerApprovalRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerCompleteTaskRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerLeaseResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerTaskEventRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerTaskEventResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.mapper.v1.TaskHttpMapper;
import io.github.jvpgjava.creatingj.task.application.port.in.ExecuteRunnerTaskProtocolUseCase;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runner/tasks")
public final class RunnerTaskController implements RunnerTaskApiDocs {

    private final ExecuteRunnerTaskProtocolUseCase runnerProtocol;
    private final TaskHttpMapper mapper;

    public RunnerTaskController(ExecuteRunnerTaskProtocolUseCase runnerProtocol, TaskHttpMapper mapper) {
        this.runnerProtocol = runnerProtocol;
        this.mapper = mapper;
    }

    @Override
    @PostMapping("/lease")
    public ResponseEntity<RunnerLeaseResponse> leaseNext(@AuthenticationPrincipal AuthenticatedDevice device) {
        return runnerProtocol.leaseNext(device.deviceId())
                .map(task -> ResponseEntity.ok(new RunnerLeaseResponse(true, mapper.toSummary(task))))
                .orElseGet(() -> ResponseEntity.ok(new RunnerLeaseResponse(false, null)));
    }

    @Override
    @PostMapping("/{taskId}/events")
    public ResponseEntity<RunnerTaskEventResponse> reportEvent(
            @PathVariable String taskId,
            @Valid @RequestBody RunnerTaskEventRequest request,
            @AuthenticationPrincipal AuthenticatedDevice device
    ) {
        var result = runnerProtocol.reportEvent(new ExecuteRunnerTaskProtocolUseCase.ReportEventCommand(
                device.deviceId(),
                TaskId.parse(taskId),
                request.eventKey(),
                request.message()
        ));
        return ResponseEntity.ok(mapper.toRunnerEvent(result));
    }

    @Override
    @PostMapping("/{taskId}/approvals")
    public ResponseEntity<ToolApprovalResponse> requestApproval(
            @PathVariable String taskId,
            @Valid @RequestBody RunnerApprovalRequest request,
            @AuthenticationPrincipal AuthenticatedDevice device
    ) {
        int ttl = request.ttlSeconds() == null ? 180 : request.ttlSeconds();
        var approval = runnerProtocol.requestApproval(new ExecuteRunnerTaskProtocolUseCase.RequestApprovalCommand(
                device.deviceId(),
                TaskId.parse(taskId),
                request.toolName(),
                request.targetPath(),
                request.commandText(),
                request.diffPreview(),
                ttl
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toApproval(approval));
    }

    @Override
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<RemoteTaskSummaryResponse> completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody RunnerCompleteTaskRequest request,
            @AuthenticationPrincipal AuthenticatedDevice device
    ) {
        var finished = runnerProtocol.completeTask(new ExecuteRunnerTaskProtocolUseCase.CompleteTaskCommand(
                device.deviceId(),
                TaskId.parse(taskId),
                request.succeeded(),
                request.resultSummary(),
                request.errorCode()
        ));
        return ResponseEntity.ok(mapper.toSummary(finished));
    }
}
