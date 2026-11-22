package io.github.jvpgjava.creatingj.common.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class OutputSecurityGuard {

    public static final String FLAG_PROMPT_LEAK = "prompt_leak";
    public static final String FLAG_REF_LEAK = "ref_leak";
    public static final String FLAG_SECRET_REDACTED = "secret_redacted";
    public static final String FLAG_DESTRUCTIVE_SNIPPET = "destructive_snippet";

    private static final List<String> PROMPT_LEAK_FRAGMENTS = List.of(
            "REGRAS INTERNAS IMUTAVEIS DO CREATINGJ",
            "MODO ESTRITO DE SEGURANCA CREATINGJ",
            "DAN ativada",
            "modo desenvolvedor ativado",
            "developer mode enabled"
    );

    private static final Pattern OPAQUE_RAG_REF = Pattern.compile(
            "(?<![\\w\\])])\\[\\s*T\\d+\\b[^\\]\\n]{0,120}\\]"
    );

    private static final List<Pattern> SECRET_REDACTION_PATTERNS = List.of(
            Pattern.compile("-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z0-9 ]*PRIVATE KEY-----"),
            Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
            Pattern.compile("\\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}\\b"),
            Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{20,}\\b"),
            Pattern.compile("\\bsk-[A-Za-z0-9_-]{20,}\\b"),
            Pattern.compile("\\b(?:sk|stac|crj)_[A-Za-z0-9_-]{16,}\\b"),
            Pattern.compile("(?i)\\b(?:CRJ_API_KEY|STAC_API_KEY|AWS_SECRET_ACCESS_KEY|CREATINGJ_DB_PASSWORD)"
                    + "\\s*=\\s*\\S+")
    );

    private static final Pattern DESTRUCTIVE_SHELL_BOMB = Pattern.compile(
            "(?i)(?:rm\\s+-rf\\s+/(?:\\s|$|\\*)|:\\(\\)\\{\\s*:\\|:\\s*&\\s*\\};:|mkfs\\.[a-z0-9]+\\s+/dev/)"
    );

    private OutputSecurityGuard() {
    }

    public static SanitizedOutput inspectAndSanitize(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return new SanitizedOutput("(sem resposta textual)", List.of(), false);
        }
        List<String> flags = new ArrayList<>();
        String lower = rawOutput.toLowerCase(Locale.ROOT);
        for (String fragment : PROMPT_LEAK_FRAGMENTS) {
            if (lower.contains(fragment.toLowerCase(Locale.ROOT))) {
                flags.add(FLAG_PROMPT_LEAK + ":" + fragment);
                return new SanitizedOutput(
                        PromptSecurityGuard.STANDARD_REFUSAL_MESSAGE,
                        List.copyOf(flags),
                        true
                );
            }
        }

        String sanitized = rawOutput;
        if (OPAQUE_RAG_REF.matcher(sanitized).find()) {
            flags.add(FLAG_REF_LEAK);
            sanitized = OPAQUE_RAG_REF.matcher(sanitized).replaceAll("");
        }

        boolean secretFound = false;
        for (Pattern secretPattern : SECRET_REDACTION_PATTERNS) {
            if (secretPattern.matcher(sanitized).find()) {
                secretFound = true;
                sanitized = secretPattern.matcher(sanitized).replaceAll("[REDACTED_SECRET]");
            }
        }
        if (secretFound) {
            flags.add(FLAG_SECRET_REDACTED);
        }

        if (DESTRUCTIVE_SHELL_BOMB.matcher(sanitized).find()) {
            flags.add(FLAG_DESTRUCTIVE_SNIPPET);
            sanitized = DESTRUCTIVE_SHELL_BOMB.matcher(sanitized)
                    .replaceAll("[COMANDO_DESTRUTIVO_BLOQUEADO]");
        }

        String finalContent = sanitized.isBlank() ? "(sem resposta textual)" : sanitized;
        return new SanitizedOutput(finalContent, List.copyOf(flags), false);
    }

    public record SanitizedOutput(
            String content,
            List<String> flags,
            boolean replaced
    ) {
    }
}
