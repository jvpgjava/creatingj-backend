package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

public record ToolApprovalDecisionResponse(
        RemoteTaskSummaryResponse task,
        ToolApprovalResponse approval
) {
}
