package io.github.jvpgjava.creatingj.task.application.port.in;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;

import java.util.List;

public interface ManageUserRemoteTasksUseCase {

    CreateResult create(CreateCommand command);

    List<RemoteTask> list(UserId actorUserId, int limit);

    TaskDetail getDetail(UserId actorUserId, TaskId taskId);

    RemoteTask cancel(UserId actorUserId, TaskId taskId);

    DecisionResult decideApproval(DecisionCommand command);

    record CreateCommand(
            UserId actorUserId,
            DeviceId deviceId,
            ProjectId projectId,
            String idempotencyKey,
            String title,
            String instruction
    ) {
    }

    record CreateResult(RemoteTask task, boolean deviceOnline, boolean deduplicated) {
    }

    record TaskDetail(RemoteTask task, List<TaskEvent> events, List<ToolApproval> approvals) {
    }

    record DecisionCommand(
            UserId actorUserId,
            TaskId taskId,
            ToolApprovalId approvalId,
            ToolDecisionAction action
    ) {
    }

    record DecisionResult(RemoteTask task, ToolApproval approval) {
    }
}
