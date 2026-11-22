package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado da busca vetorial RAG filtrada por proprietario e projeto")
public record KnowledgeSearchResponse(
        @Schema(description = "Identificador do projeto pesquisado", example = "550e8400-e29b-41d4-a716-446655440010")
        String projectId,
        @Schema(description = "Versao do modelo de embedding utilizada na busca",
                example = "onnx:multilingual-e5-base-q8:768")
        String embeddingVersion,
        @Schema(description = "Latencia total da busca vetorial em milissegundos", example = "18")
        long latencyMs,
        @Schema(description = "Trechos mais relevantes ordenados por similaridade decrescente")
        List<KnowledgeSearchMatchResponse> matches
) {
}
