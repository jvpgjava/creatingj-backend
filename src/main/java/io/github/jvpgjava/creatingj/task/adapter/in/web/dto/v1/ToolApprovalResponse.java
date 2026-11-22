package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pedido temporario de aprovacao de ferramenta submetido pelo runner durante uma tarefa remota")
public record ToolApprovalResponse(
        @Schema(description = "Identificador do pedido de aprovacao",
                example = "550e8400-e29b-41d4-a716-446655440062")
        String id,
        @Schema(description = "Identificador da tarefa associada",
                example = "550e8400-e29b-41d4-a716-446655440060")
        String taskId,
        @Schema(description = "Nome da ferramenta solicitada", example = "run_command")
        String toolName,
        @Schema(description = "Caminho relativo alvo no workspace",
                example = "src/main/java/io/github/jvpgjava/creatingj/bootstrap/config/OpenApiConfig.java",
                nullable = true)
        String targetPath,
        @Schema(description = "Comando solicitado para execucao local", example = "mvn -B test", nullable = true)
        String commandText,
        @Schema(description = "Pre-visualizacao do diff ou alteracao proposta",
                example = "--- a/pom.xml\n+++ b/pom.xml", nullable = true)
        String diffPreview,
        @Schema(description = "Estado do pedido (PENDING, APPROVED, REJECTED, EXPIRED, DENIED_BY_POLICY)",
                example = "PENDING")
        String status,
        @Schema(description = "Instante UTC de expiracao do pedido", example = "2026-11-22T18:24:00Z")
        Instant expiresAt,
        @Schema(description = "Instante UTC da decisao humana (nulo enquanto pendente)", nullable = true)
        Instant decidedAt,
        @Schema(description = "Instante UTC de criacao do pedido", example = "2026-11-22T18:21:00Z")
        Instant createdAt
) {
}
