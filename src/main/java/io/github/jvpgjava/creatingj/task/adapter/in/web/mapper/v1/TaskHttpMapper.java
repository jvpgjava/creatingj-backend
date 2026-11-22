package io.github.jvpgjava.creatingj.task.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.CreateRemoteTaskRequest;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.CreateRemoteTaskResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskDetailResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTaskSummaryResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RemoteTasksResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.RunnerTaskEventResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.TaskEventResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalDecisionResponse;
import io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1.ToolApprovalResponse;
import io.github.jvpgjava.creatingj.task.application.port.in.ExecuteRunnerTaskProtocolUseCase;
import io.github.jvpgjava.creatingj.task.application.port.in.ManageUserRemoteTasksUseCase;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;

import java.util.List;

public final class TaskHttpMapper {

    public ManageUserRemoteTasksUseCase.CreateCommand toCreateCommand(
            UserId actorUserId,
            CreateRemoteTaskRequest request
    ) {
        ProjectId projectId = (request.projectId() == null || request.projectId().isBlank())
                ? null
                : ProjectId.parse(request.projectId());
        return new ManageUserRemoteTasksUseCase.CreateCommand(
                actorUserId,
                DeviceId.parse(request.deviceId()),
                projectId,
                request.idempotencyKey(),
                request.title(),
                request.instruction()
        );
    }

    public RemoteTaskSummaryResponse toSummary(RemoteTask task) {
        return new RemoteTaskSummaryResponse(
                task.id().toString(),
                task.projectId() == null ? null : task.projectId().toString(),
                task.deviceId().toString(),
                task.title(),
                task.instruction(),
                task.status().name(),
                task.leaseExpiresAt(),
                task.resultSummary(),
                task.errorCode(),
                task.createdAt(),
                task.updatedAt(),
                task.completedAt()
        );
    }

    public CreateRemoteTaskResponse toCreateResponse(ManageUserRemoteTasksUseCase.CreateResult result) {
        return new CreateRemoteTaskResponse(toSummary(result.task()), result.deviceOnline(), result.deduplicated());
    }

    public TaskEventResponse toEvent(TaskEvent event) {
        return new TaskEventResponse(
                event.id().toString(),
                event.eventKey(),
                event.eventType().name(),
                event.message(),
                event.createdAt()
        );
    }

    public ToolApprovalResponse toApproval(ToolApproval approval) {
        return new ToolApprovalResponse(
                approval.id().toString(),
                approval.taskId().toString(),
                approval.toolName().name(),
                approval.targetPath(),
                approval.commandText(),
                approval.diffPreview(),
                approval.status().name(),
                approval.expiresAt(),
                approval.decidedAt(),
                approval.createdAt()
        );
    }

    public RemoteTaskDetailResponse toDetail(ManageUserRemoteTasksUseCase.TaskDetail detail) {
        return new RemoteTaskDetailResponse(
                toSummary(detail.task()),
                detail.events().stream().map(this::toEvent).toList(),
                detail.approvals().stream().map(this::toApproval).toList()
        );
    }

    public RemoteTasksResponse toTasks(List<RemoteTask> tasks) {
        return new RemoteTasksResponse(tasks.stream().map(this::toSummary).toList());
    }

    public ToolApprovalDecisionResponse toDecision(ManageUserRemoteTasksUseCase.DecisionResult result) {
        return new ToolApprovalDecisionResponse(toSummary(result.task()), toApproval(result.approval()));
    }

    public RunnerTaskEventResponse toRunnerEvent(ExecuteRunnerTaskProtocolUseCase.EventResult result) {
        return new RunnerTaskEventResponse(toEvent(result.event()), result.deduplicated());
    }
}
