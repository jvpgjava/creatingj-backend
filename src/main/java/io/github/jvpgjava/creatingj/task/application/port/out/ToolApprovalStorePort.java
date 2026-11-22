package io.github.jvpgjava.creatingj.task.application.port.out;

import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;

import java.util.List;
import java.util.Optional;

public interface ToolApprovalStorePort {

    ToolApproval save(ToolApproval approval);

    Optional<ToolApproval> findById(ToolApprovalId approvalId);

    List<ToolApproval> findByTaskOrderByCreatedAsc(TaskId taskId);
}
