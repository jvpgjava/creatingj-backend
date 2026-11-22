package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Proposta individual de uso de ferramenta planejada pelo agente")
public record AgentToolProposalResponse(
        @Schema(description = "Identificador da proposta", example = "550e8400-e29b-41d4-a716-446655440041")
        String id,
        @Schema(description = "Ordem sequencial do passo no plano", example = "1")
        int stepIndex,
        @Schema(description = "Nome da ferramenta proposta (read_file, write_file, run_command, etc.)",
                example = "write_file")
        String toolName,
        @Schema(description = "Caminho relativo alvo no workspace do cliente",
                example = "src/test/java/io/github/jvpgjava/creatingj/web/CorsPolicyTest.java")
        String targetPath,
        @Schema(description = "Comando sugerido quando a ferramenta for de execucao (nulo para leitura/escrita)",
                example = "mvn -B test -Dtest=CorsPolicyTest", nullable = true)
        String commandText,
        @Schema(description = "Justificativa gerada pelo modelo para a proposta",
                example = "Garantir que origens nao autorizadas sejam bloqueadas pelo filtro CORS")
        String rationale,
        @Schema(description = "Veredito da politica deterministica (ALLOW, REQUIRE_APPROVAL, DENY)",
                example = "REQUIRE_APPROVAL")
        String policyDecision,
        @Schema(description = "Motivo tecnico da classificacao de politica",
                example = "Escrita de arquivo no workspace exige aprovacao explicita do usuario")
        String policyReason,
        @Schema(description = "Estado da aprovacao (AUTO_APPROVED, PENDING, APPROVED, REJECTED, DENIED_BY_POLICY)",
                example = "PENDING")
        String approvalStatus,
        @Schema(description = "Instante UTC da decisao humana (nulo enquanto pendente)",
                example = "2026-11-22T18:16:00Z", nullable = true)
        Instant decidedAt,
        @Schema(description = "Instante UTC de criacao da proposta", example = "2026-11-22T18:15:02Z")
        Instant createdAt
) {
}
