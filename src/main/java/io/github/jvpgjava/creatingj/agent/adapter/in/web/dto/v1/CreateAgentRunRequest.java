package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Payload para iniciar uma execucao de planejamento do agente no projeto")
public record CreateAgentRunRequest(
        @Schema(description = "Objetivo claro que o agente deve planejar para o cliente executar",
                example = "Revisar a configuracao de CORS e propor um teste unitario para origens invalidas")
        @NotBlank(message = "Objetivo do agente obrigatorio")
        @Size(max = 2000, message = "Objetivo deve ter no maximo 2000 caracteres")
        String goal,
        @Schema(description = "Modelo opcional para sobrescrever o padrao do provedor",
                example = "z-ai/glm-5.3-flash")
        @Size(max = 120, message = "Identificador do modelo invalido")
        String modelId,
        @Schema(description = "Snapshots de arquivos do workspace do cliente (ate 10 arquivos; caminhos sensiveis "
                + "sao bloqueados)")
        @Size(max = 10, message = "No maximo 10 arquivos de workspace por execucao")
        List<@Valid WorkspaceFileSnapshotRequest> workspaceFiles,
        @Schema(description = "Indica se o backend deve enriquecer o contexto com busca RAG do projeto",
                example = "true")
        Boolean includeProjectRag,
        @Schema(description = "Quantidade maxima de trechos RAG injetados (1..10)", example = "3")
        @Min(value = 1, message = "ragTopK minimo e 1")
        @Max(value = 10, message = "ragTopK maximo e 10")
        Integer ragTopK
) {
}
