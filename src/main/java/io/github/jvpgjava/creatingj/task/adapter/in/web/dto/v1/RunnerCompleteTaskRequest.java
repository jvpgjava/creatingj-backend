package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import jakarta.validation.constraints.Size;

public record RunnerCompleteTaskRequest(
        boolean succeeded,
        @Size(max = 4000, message = "Resumo excede o limite")
        String resultSummary,
        @Size(max = 64, message = "Codigo de erro excede o limite")
        String errorCode
) {
}
