package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

public record RunnerLeaseResponse(
        boolean assigned,
        RemoteTaskSummaryResponse task
) {
}
