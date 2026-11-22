package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import java.util.List;

public record RemoteTaskDetailResponse(
        RemoteTaskSummaryResponse task,
        List<TaskEventResponse> events,
        List<ToolApprovalResponse> approvals
) {
}
