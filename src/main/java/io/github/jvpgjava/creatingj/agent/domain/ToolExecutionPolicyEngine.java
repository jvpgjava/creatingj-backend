package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIngestionPolicy;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ToolExecutionPolicyEngine {

    private static final List<String> ALLOWED_COMMAND_PREFIXES = List.of(
            "mvn test",
            "mvn verify",
            "./mvnw test",
            "./mvnw verify",
            "mvnw test",
            "mvnw verify",
            "npm test",
            "npm run build",
            "npm run lint",
            "cargo test",
            "cargo check",
            "git status",
            "git diff",
            "git log"
    );

    private static final List<Pattern> FORBIDDEN_COMMAND_PATTERNS = List.of(
            Pattern.compile("[;|&`<>\\n\\r]"),
            Pattern.compile("\\$\\("),
            Pattern.compile("(?i)\\b(?:curl|wget|nc|ncat|ssh|scp|sftp|telnet|ftp|invoke-webrequest|iwr)\\b"),
            Pattern.compile("(?i)\\b(?:sudo|su|chmod|chown|mkfs|dd|shutdown|reboot|powershell|cmd\\.exe)\\b"),
            Pattern.compile("(?i)\\b(?:rm\\s+-rf|del\\s+/f|rd\\s+/s|remove-item)\\b"),
            Pattern.compile("(?i)(?:\\.env|printenv|id_rsa|private_key|crj_api_key|stac_api_key)")
    );

    private ToolExecutionPolicyEngine() {
    }

    public static Evaluation evaluate(ProposedToolAction action) {
        if (action == null) {
            return new Evaluation(
                    AgentToolName.UNKNOWN,
                    null,
                    null,
                    ToolPolicyDecision.DENIED_BY_POLICY,
                    ToolApprovalStatus.DENIED,
                    "Proposta de ferramenta ausente."
            );
        }
        AgentToolName tool = AgentToolName.fromRaw(action.rawToolName());
        return switch (tool) {
            case READ_FILE, SEARCH_CODE -> evaluateReadTool(tool, action.targetPath());
            case WRITE_FILE -> evaluateWriteTool(action.targetPath());
            case RUN_COMMAND -> evaluateCommandTool(action.commandText());
            case UNKNOWN -> new Evaluation(
                    AgentToolName.UNKNOWN,
                    action.targetPath(),
                    action.commandText(),
                    ToolPolicyDecision.DENIED_BY_POLICY,
                    ToolApprovalStatus.DENIED,
                    "Ferramenta nao autorizada na allowlist."
            );
        };
    }

    private static Evaluation evaluateReadTool(AgentToolName tool, String rawPath) {
        if (tool == AgentToolName.SEARCH_CODE && (rawPath == null || rawPath.isBlank())) {
            return new Evaluation(
                    tool,
                    null,
                    null,
                    ToolPolicyDecision.AUTO_ALLOWED,
                    ToolApprovalStatus.AUTO_APPROVED,
                    "Busca somente leitura no workspace permitida."
            );
        }
        try {
            String normalizedPath = KnowledgeIngestionPolicy.normalizeAndValidatePath(rawPath);
            return new Evaluation(
                    tool,
                    normalizedPath,
                    null,
                    ToolPolicyDecision.AUTO_ALLOWED,
                    ToolApprovalStatus.AUTO_APPROVED,
                    "Leitura permitida para caminho seguro do workspace."
            );
        } catch (DomainException ex) {
            return new Evaluation(
                    tool,
                    rawPath,
                    null,
                    ToolPolicyDecision.DENIED_BY_POLICY,
                    ToolApprovalStatus.DENIED,
                    "Caminho bloqueado por politica de seguranca do workspace."
            );
        }
    }

    private static Evaluation evaluateWriteTool(String rawPath) {
        try {
            String normalizedPath = KnowledgeIngestionPolicy.normalizeAndValidatePath(rawPath);
            return new Evaluation(
                    AgentToolName.WRITE_FILE,
                    normalizedPath,
                    null,
                    ToolPolicyDecision.REQUIRES_APPROVAL,
                    ToolApprovalStatus.PENDING,
                    "Alteracao de arquivo exige aprovacao explicita do usuario."
            );
        } catch (DomainException ex) {
            return new Evaluation(
                    AgentToolName.WRITE_FILE,
                    rawPath,
                    null,
                    ToolPolicyDecision.DENIED_BY_POLICY,
                    ToolApprovalStatus.DENIED,
                    "Escrita em caminho sensivel ou fora do workspace bloqueada."
            );
        }
    }

    private static Evaluation evaluateCommandTool(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return new Evaluation(
                    AgentToolName.RUN_COMMAND,
                    null,
                    rawCommand,
                    ToolPolicyDecision.DENIED_BY_POLICY,
                    ToolApprovalStatus.DENIED,
                    "Comando vazio nao permitido."
            );
        }
        String trimmed = rawCommand.trim();
        for (Pattern forbidden : FORBIDDEN_COMMAND_PATTERNS) {
            if (forbidden.matcher(trimmed).find()) {
                return new Evaluation(
                        AgentToolName.RUN_COMMAND,
                        null,
                        trimmed,
                        ToolPolicyDecision.DENIED_BY_POLICY,
                        ToolApprovalStatus.DENIED,
                        "Comando bloqueado por politica anti-exfiltracao/destruicao."
                );
            }
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        boolean matchesAllowlist = ALLOWED_COMMAND_PREFIXES.stream()
                .anyMatch(prefix -> lower.equals(prefix) || lower.startsWith(prefix + " "));
        if (!matchesAllowlist) {
            return new Evaluation(
                    AgentToolName.RUN_COMMAND,
                    null,
                    trimmed,
                    ToolPolicyDecision.DENIED_BY_POLICY,
                    ToolApprovalStatus.DENIED,
                    "Comando fora da allowlist permitida para execucao."
            );
        }
        return new Evaluation(
                AgentToolName.RUN_COMMAND,
                null,
                trimmed,
                ToolPolicyDecision.REQUIRES_APPROVAL,
                ToolApprovalStatus.PENDING,
                "Comando na allowlist exige aprovacao explicita do usuario."
        );
    }

    public record Evaluation(
            AgentToolName toolName,
            String normalizedTargetPath,
            String sanitizedCommand,
            ToolPolicyDecision policyDecision,
            ToolApprovalStatus initialApprovalStatus,
            String policyReason
    ) {
    }
}
