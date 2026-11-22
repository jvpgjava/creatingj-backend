package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Registro persistido de execucao de job de indexacao ou reindexacao vetorial")
public record KnowledgeIndexJobResponse(
        @Schema(description = "Identificador do job", example = "550e8400-e29b-41d4-a716-446655440031")
        String id,
        @Schema(description = "Identificador do projeto", example = "550e8400-e29b-41d4-a716-446655440010")
        String projectId,
        @Schema(description = "Identificador da fonte indexada", example = "550e8400-e29b-41d4-a716-446655440030")
        String sourceId,
        @Schema(description = "Caminho relativo da fonte", example = "docs/arquitetura-auth.md")
        String sourcePath,
        @Schema(description = "Operacao executada (INGEST, REINDEX, DELETE)", example = "INGEST")
        String operation,
        @Schema(description = "Status do job (COMPLETED, DEDUPLICATED, FAILED)", example = "COMPLETED")
        String status,
        @Schema(description = "Quantidade de chunks vetorizados gravados no pgvector", example = "4")
        int chunksIndexed,
        @Schema(description = "Codigo de erro quando status for FAILED (nulo em sucesso)", nullable = true)
        String errorCode,
        @Schema(description = "Instante UTC de criacao do job", example = "2026-11-22T18:10:00Z")
        Instant createdAt,
        @Schema(description = "Instante UTC de conclusao do job", example = "2026-11-22T18:10:01Z")
        Instant completedAt
) {
}
