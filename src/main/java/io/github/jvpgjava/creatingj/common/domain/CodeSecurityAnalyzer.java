package io.github.jvpgjava.creatingj.common.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class CodeSecurityAnalyzer {

    public static final String FLAG_PREFIX = "insecure_code_pattern:";

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "sql_concatenation",
                    true,
                    Pattern.compile("(?i)(?:SELECT|INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\b[^;\\n]{0,120}"
                            + "(?:[\"']\\s*\\+\\s*[a-zA-Z_]|\\$\\{[a-zA-Z_]|\\.[a-zA-Z_]+\\s*\\.\\s*format\\()"),
                    "Evite concatenacao ou interpolacao direta em SQL; utilize queries parametrizadas/prepared "
                            + "statements."
            ),
            new Rule(
                    "dangerous_eval",
                    true,
                    Pattern.compile("(?i)\\b(?:eval|exec)\\s*\\([^)\\n]{1,120}\\)|new\\s+Function\\s*\\("),
                    "Evite avaliacao dinamica de codigo (eval/exec/new Function); utilize parsers seguros."
            ),
            new Rule(
                    "xss_raw_html_sink",
                    false,
                    Pattern.compile("(?i)(?:\\.(?:innerHTML|outerHTML)\\s*=|dangerouslySetInnerHTML|"
                            + "v-html\\s*=|\\[innerHTML\\]\\s*=|document\\.write\\s*\\()"),
                    "Atribuicao direta de HTML nao sanitizado pode introduzir XSS; sanitize ou use binding seguro."
            ),
            new Rule(
                    "tls_verification_disabled",
                    true,
                    Pattern.compile("(?i)(?:InsecureSkipVerify\\s*:\\s*true|verify\\s*=\\s*False|"
                            + "NODE_TLS_REJECT_UNAUTHORIZED\\s*=\\s*['\"]?0|TrustAllCerts|NoopHostnameVerifier|"
                            + "ServerCertificateCustomValidationCallback[^;\\n]{0,80}=>\\s*true)"),
                    "Nunca desative a validacao de certificados TLS/SSL em codigo de aplicacao."
            ),
            new Rule(
                    "weak_password_hash",
                    false,
                    Pattern.compile("(?i)(?:MessageDigest\\.getInstance\\s*\\(\\s*\"(?:MD5|SHA-?1)\"|"
                            + "hashlib\\.(?:md5|sha1)\\s*\\([^)\\n]*pass|md5\\s*\\(\\s*\\$pass)"),
                    "MD5/SHA-1 sao inadequados para senhas; utilize Argon2id, bcrypt ou scrypt."
            ),
            new Rule(
                    "insecure_deserialization",
                    true,
                    Pattern.compile("(?i)(?:new\\s+ObjectInputStream\\s*\\([^)\\n]+\\)\\.readObject\\s*\\(|"
                            + "pickle\\.loads?\\s*\\(|yaml\\.load\\s*\\([^)\\n]+\\)(?!.*SafeLoader)|"
                            + "BinaryFormatter\\s*\\(\\s*\\)\\.Deserialize)"),
                    "Desserializacao nativa nao confiavel permite RCE; utilize formatos seguros e schemas tipados."
            )
    );

    private CodeSecurityAnalyzer() {
    }

    public static AnalysisResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return new AnalysisResult(List.of(), List.of(), false);
        }
        Set<String> flags = new LinkedHashSet<>();
        List<String> advisories = new ArrayList<>();
        boolean criticalFound = false;

        for (Rule rule : RULES) {
            if (rule.pattern().matcher(text).find()) {
                flags.add(FLAG_PREFIX + rule.id());
                advisories.add(rule.advisory());
                if (rule.criticalForDiff()) {
                    criticalFound = true;
                }
            }
        }
        return new AnalysisResult(List.copyOf(flags), List.copyOf(advisories), criticalFound);
    }

    public static String formatAdvisoryBlock(List<String> advisories) {
        if (advisories == null || advisories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n---\n**[Revisao Automatica de Seguranca CreatingJ]**\n");
        for (String advisory : advisories) {
            sb.append("- ").append(advisory).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private record Rule(
            String id,
            boolean criticalForDiff,
            Pattern pattern,
            String advisory
    ) {
    }

    public record AnalysisResult(
            List<String> flags,
            List<String> advisories,
            boolean hasCriticalFinding
    ) {
    }
}
