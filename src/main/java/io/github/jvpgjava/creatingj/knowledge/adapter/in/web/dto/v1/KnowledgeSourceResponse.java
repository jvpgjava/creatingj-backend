package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Metadados de uma fonte de conhecimento indexada no projeto")
public record KnowledgeSourceResponse(
        @Schema(description = "Identificador da fonte", example = "550e8400-e29b-41d4-a716-446655440030")
        String id,
        @Schema(description = "Identificador do projeto", example = "550e8400-e29b-41d4-a716-446655440010")
        String projectId,
        @Schema(description = "Caminho relativo da fonte no projeto", example = "docs/arquitetura-auth.md")
        String sourcePath,
        @Schema(description = "Titulo da fonte", example = "Arquitetura de Autenticacao")
        String title,
        @Schema(description = "Hash SHA-256 do conteudo para deduplicacao",
                example = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4")
        String contentHash,
        @Schema(description = "Versao do embedding aplicada aos chunks atuais",
                example = "onnx:multilingual-e5-base-q8:768")
        String embeddingVersion,
        @Schema(description = "Estado atual da fonte (INDEXED, FAILED)", example = "INDEXED")
        String status,
        @Schema(description = "Quantidade de chunks ativos no pgvector", example = "4")
        int chunkCount,
        @Schema(description = "Instante UTC de criacao", example = "2026-11-22T18:10:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC da ultima reindexacao", example = "2026-11-22T18:10:01Z")
        Instant updatedAt
) {
}
