package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

public record RunnerTaskEventResponse(
        TaskEventResponse event,
        boolean deduplicated
) {
}
