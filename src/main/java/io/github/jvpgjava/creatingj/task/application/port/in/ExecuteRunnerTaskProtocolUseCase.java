package io.github.jvpgjava.creatingj.task.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;

import java.util.Optional;

public interface ExecuteRunnerTaskProtocolUseCase {

    Optional<RemoteTask> leaseNext(DeviceId deviceId);

    EventResult reportEvent(ReportEventCommand command);

    ToolApproval requestApproval(RequestApprovalCommand command);

    RemoteTask completeTask(CompleteTaskCommand command);

    record ReportEventCommand(DeviceId deviceId, TaskId taskId, String eventKey, String message) {
    }

    record EventResult(TaskEvent event, boolean deduplicated) {
    }

    record RequestApprovalCommand(
            DeviceId deviceId,
            TaskId taskId,
            String toolName,
            String targetPath,
            String commandText,
            String diffPreview,
            int ttlSeconds
    ) {
    }

    record CompleteTaskCommand(
            DeviceId deviceId,
            TaskId taskId,
            boolean succeeded,
            String resultSummary,
            String errorCode
    ) {
    }
}
