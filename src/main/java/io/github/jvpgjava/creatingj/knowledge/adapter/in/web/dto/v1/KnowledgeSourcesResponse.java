package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lista de fontes de conhecimento indexadas do projeto")
public record KnowledgeSourcesResponse(
        @Schema(description = "Fontes indexadas pertencentes ao projeto")
        List<KnowledgeSourceResponse> items
) {
}
