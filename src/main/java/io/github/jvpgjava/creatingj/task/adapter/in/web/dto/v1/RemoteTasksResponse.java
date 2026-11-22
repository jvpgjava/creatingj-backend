package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lista de tarefas remotas do usuario autenticado")
public record RemoteTasksResponse(
        @Schema(description = "Tarefas ordenadas da mais recente para a mais antiga")
        List<RemoteTaskSummaryResponse> items
) {
}
