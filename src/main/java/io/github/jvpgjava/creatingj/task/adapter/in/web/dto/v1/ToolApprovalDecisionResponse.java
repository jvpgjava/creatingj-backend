package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado atualizado da tarefa e do pedido de aprovacao apos decisao do usuario")
public record ToolApprovalDecisionResponse(
        @Schema(description = "Resumo atualizado da tarefa remota")
        RemoteTaskSummaryResponse task,
        @Schema(description = "Pedido de aprovacao com decisao registrada")
        ToolApprovalResponse approval
) {
}
