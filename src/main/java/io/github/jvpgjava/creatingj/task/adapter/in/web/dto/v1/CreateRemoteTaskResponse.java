package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

public record CreateRemoteTaskResponse(
        RemoteTaskSummaryResponse task,
        boolean deviceOnline,
        boolean deduplicated
) {
}
