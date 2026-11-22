package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAgentRunRequest(
        @NotBlank(message = "Objetivo do agente obrigatorio")
        @Size(max = 2000, message = "Objetivo deve ter no maximo 2000 caracteres")
        String goal,
        @Size(max = 120, message = "Identificador do modelo invalido")
        String modelId,
        @Valid
        @Size(max = 10, message = "No maximo 10 arquivos de workspace por execucao")
        List<WorkspaceFileSnapshotRequest> workspaceFiles,
        Boolean includeProjectRag,
        @Min(value = 1, message = "ragTopK minimo e 1")
        @Max(value = 10, message = "ragTopK maximo e 10")
        Integer ragTopK
) {
}
