package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import java.util.List;

public record RemoteTasksResponse(
        List<RemoteTaskSummaryResponse> items
) {
}
