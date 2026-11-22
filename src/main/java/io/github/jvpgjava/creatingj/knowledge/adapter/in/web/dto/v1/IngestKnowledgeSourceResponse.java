package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da ingestao de uma fonte de conhecimento e respectivo job de indexacao")
public record IngestKnowledgeSourceResponse(
        @Schema(description = "Metadados da fonte indexada")
        KnowledgeSourceResponse source,
        @Schema(description = "Job de indexacao registrado para auditoria")
        KnowledgeIndexJobResponse job,
        @Schema(description = "Indica se o conteudo era identico ao hash atual e foi deduplicado sem reembedding",
                example = "false")
        boolean deduplicated
) {
}
