package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trecho recuperado por similaridade vetorial com citacao da fonte original")
public record KnowledgeSearchMatchResponse(
        @Schema(description = "Identificador da fonte", example = "550e8400-e29b-41d4-a716-446655440030")
        String sourceId,
        @Schema(description = "Caminho relativo da fonte", example = "docs/arquitetura-auth.md")
        String sourcePath,
        @Schema(description = "Titulo da fonte", example = "Arquitetura de Autenticacao")
        String title,
        @Schema(description = "Indice sequencial do chunk dentro da fonte", example = "0")
        int chunkIndex,
        @Schema(description = "Trecho textual retornado para composicao de contexto RAG",
                example = "A autenticacao web utiliza cookie HttpOnly CREATINGJ_SESSION com CSRF X-XSRF-TOKEN.")
        String excerpt,
        @Schema(description = "Similaridade de cosseno calculada pelo pgvector (entre -1.0 e 1.0)", example = "0.842")
        double score,
        @Schema(description = "Versao do modelo de embedding que gerou o vetor",
                example = "onnx:multilingual-e5-base-q8:768")
        String embeddingVersion
) {
}
