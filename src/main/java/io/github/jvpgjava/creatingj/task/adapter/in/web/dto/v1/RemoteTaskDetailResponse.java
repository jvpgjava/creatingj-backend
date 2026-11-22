package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detalhes completos de uma tarefa remota, timeline de eventos e aprovacoes pendentes")
public record RemoteTaskDetailResponse(
        @Schema(description = "Resumo atual da tarefa")
        RemoteTaskSummaryResponse task,
        @Schema(description = "Eventos cronologicos reportados pelo runner")
        List<TaskEventResponse> events,
        @Schema(description = "Pedidos de aprovacao de ferramentas associados a tarefa")
        List<ToolApprovalResponse> approvals
) {
}
