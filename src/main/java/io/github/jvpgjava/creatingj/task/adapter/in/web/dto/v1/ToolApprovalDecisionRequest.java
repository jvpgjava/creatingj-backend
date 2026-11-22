package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Decisao humana de uso unico sobre um pedido de aprovacao de ferramenta da tarefa remota")
public record ToolApprovalDecisionRequest(
        @Schema(description = "Acao aplicada ao pedido (APPROVE ou REJECT)", example = "APPROVE")
        @NotNull(message = "Acao de decisao obrigatoria")
        ToolDecisionAction action
) {
}
