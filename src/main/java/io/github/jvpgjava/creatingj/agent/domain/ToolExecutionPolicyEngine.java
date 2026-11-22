package io.github.jvpgjava.creatingj.agent.domain;

import io.github.jvpgjava.creatingj.common.domain.CodeSecurityAnalyzer;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.PromptSecurityGuard;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIngestionPolicy;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ToolExecutionPolicyEngine {

    private static final List<String> ALLOWED_COMMAND_PREFIXES = List.of(
            "mvn test",
            "mvn verify",
            "mvn -b test",
            "mvn -b verify",
            "./mvnw test",
            "./mvnw verify",
            "./mvnw -b test",
            "./mvnw -b verify",
            "mvnw test",
            "mvnw verify",
            "mvnw -b test",
            "mvnw -b verify",
            "gradle test",
            "gradle check",
            "./gradlew test",
            "./gradlew check",
            "gradlew test",
            "gradlew check",
            "npm test",
            "npm run build",
            "npm run lint",
            "npm run typecheck",
            "pnpm test",
            "pnpm build",
            "pnpm lint",
            "pnpm typecheck",
            "yarn test",
            "yarn build",
            "yarn lint",
            "npx vitest run",
            "npx jest",
            "npx tsc --noemit",
            "npx eslint",
            "npx ng test",
            "npx ng build",
            "deno test",
            "deno check",
            "deno lint",
            "bun test",
            "bun run build",
            "bun run lint",
            "pytest",
            "python -m pytest",
            "python3 -m pytest",
            "python -m unittest",
            "python3 -m unittest",
            "ruff check",
            "flake8",
            "mypy",
            "black --check",
            "cargo test",
            "cargo check",
            "cargo clippy",
            "cargo build",
            "go test",
            "go vet",
            "go build",
            "golangci-lint run",
            "dotnet test",
            "dotnet build",
            "dotnet format --verify-no-changes",
            "composer test",
            "vendor/bin/phpunit",
            "php artisan test",
            "vendor/bin/phpstan",
            "bundle exec rspec",
            "bundle exec rake test",
            "rubocop",
            "flutter test",
            "flutter analyze",
            "dart test",
            "dart analyze",
            "ctest",
            "cmake --build",
            "mix test",
            "swift test",
            "swift build",
            "git status",
            "git diff",
            "git log"
    );

    private static final Set<String> FORBIDDEN_WRITE_FILENAMES = Set.of(
            "jenkinsfile",
            "dockerfile",
            "docker-compose.yml",
            "docker-compose.yaml",
            "makefile",
            "mvnw",
            "mvnw.cmd",
            "gradlew",
            "gradlew.bat",
            "authorized_keys",
            "known_hosts",
            "ssh_config"
    );

    private static final Set<String> FORBIDDEN_WRITE_PREFIXES = Set.of(
            ".github/",
            ".husky/",
            ".mvn/",
            ".gradle/",
            ".vscode/",
            ".idea/"
    );

    private static final Set<String> FORBIDDEN_WRITE_EXTENSIONS = Set.of(
            ".sh", ".bash", ".zsh", ".bat", ".cmd", ".ps1", ".vbs", ". phar"
    );

    private static final List<Pattern> FORBIDDEN_COMMAND_PATTERNS = List.of(
            Pattern.compile("[;|&`<>\\n\\r]"),
            Pattern.compile("\\$\\(|\\$\\{"),
            Pattern.compile("\\.\\./|\\.\\.\\\\"),
            Pattern.compile("(?i)\\b(?:curl|wget|nc|ncat|netcat|socat|ssh|scp|sftp|telnet|ftp|"
                    + "invoke-webrequest|iwr|invoke-restmethod|irm|certutil|bitsadmin)\\b"),
            Pattern.compile("(?i)\\b(?:sudo|su|chmod|chown|mkfs|dd|shutdown|reboot|"
                    + "powershell|pwsh|cmd\\.exe|bash|sh|zsh|wsl)\\b"),
            Pattern.compile("(?i)\\b(?:rm\\s+-rf|del\\s+/f|rd\\s+/s|remove-item|format\\s+[a-z]:)\\b"),
            Pattern.compile("(?i)\\b(?:python3?|node|deno|bun|ruby|perl|php|lua)\\s+(?:-[ce]|--eval)\\b"),
            Pattern.compile("(?i)(?:-Dexec\\.|exec:exec|exec:java|--script-shell|JAVA_TOOL_OPTIONS|"
                    + "MAVEN_OPTS|NODE_OPTIONS|LD_PRELOAD|GIT_SSH_COMMAND|GIT_EXTERNAL_DIFF|--upload-pack)"),
            Pattern.compile("(?i)(?:\\.env|printenv|id_rsa|id_ed25519|private_key|"
                    + "crj_api_key|stac_api_key|/etc/passwd|/etc/shadow|authorized_keys)")
    );

    private static final Pattern DESTRUCTIVE_DIFF_PATTERN = Pattern.compile(
            "(?i)(?:Runtime\\.getRuntime\\(\\)\\.exec|new\\s+ProcessBuilder\\s*\\(|"
                    + "rm\\s+-rf\\s+/|curl\\s+https?://[^\\s]+\\s*\\|\\s*(?:sh|bash))"
    );

    private ToolExecutionPolicyEngine() {
    }

    public static Evaluation evaluate(ProposedToolAction action) {
        return evaluateWithDiff(action, null);
    }

    public static Evaluation evaluateWithDiff(ProposedToolAction action, String diffPreview) {
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
        if (diffPreview != null && !diffPreview.isBlank()) {
            if (KnowledgeIngestionPolicy.containsSecret(diffPreview)) {
                return new Evaluation(
                        AgentToolName.fromRaw(action.rawToolName()),
                        action.targetPath(),
                        action.commandText(),
                        ToolPolicyDecision.DENIED_BY_POLICY,
                        ToolApprovalStatus.DENIED,
                        "Diff contem segredo ou credencial exposta; operacao bloqueada."
                );
            }
            if (DESTRUCTIVE_DIFF_PATTERN.matcher(diffPreview).find()
                    || PromptSecurityGuard.inspectInput(diffPreview).blocked()
                    || CodeSecurityAnalyzer.analyze(diffPreview).hasCriticalFinding()) {
                return new Evaluation(
                        AgentToolName.fromRaw(action.rawToolName()),
                        action.targetPath(),
                        action.commandText(),
                        ToolPolicyDecision.DENIED_BY_POLICY,
                        ToolApprovalStatus.DENIED,
                        "Diff contem padrao critico de vulnerabilidade, execucao arbitraria ou injecao bloqueado."
                );
            }
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
            if (isProtectedWriteTarget(normalizedPath)) {
                return new Evaluation(
                        AgentToolName.WRITE_FILE,
                        normalizedPath,
                        null,
                        ToolPolicyDecision.DENIED_BY_POLICY,
                        ToolApprovalStatus.DENIED,
                        "Escrita em script executavel, wrapper de build ou pipeline CI/CD bloqueada."
                );
            }
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

    private static boolean isProtectedWriteTarget(String normalizedPath) {
        String lower = normalizedPath.toLowerCase(Locale.ROOT);
        for (String prefix : FORBIDDEN_WRITE_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        int slashIdx = lower.lastIndexOf('/');
        String fileName = slashIdx >= 0 ? lower.substring(slashIdx + 1) : lower;
        if (FORBIDDEN_WRITE_FILENAMES.contains(fileName) || fileName.startsWith("dockerfile.")) {
            return true;
        }
        for (String ext : FORBIDDEN_WRITE_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
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
