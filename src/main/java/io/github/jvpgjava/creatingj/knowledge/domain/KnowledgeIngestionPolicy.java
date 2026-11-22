package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class KnowledgeIngestionPolicy {

    public static final int MAX_PATH_LENGTH = 260;
    public static final int MAX_CONTENT_LENGTH = 100_000;

    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(
            ".git",
            "node_modules",
            "target",
            "vendor",
            "dist",
            "build",
            ".gradle",
            ".idea"
    );

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".pem", ".key", ".p12", ".pfx", ".jks", ".keystore",
            ".exe", ".dll", ".so", ".dylib", ".jar", ".war", ".class",
            ".zip", ".tar", ".gz", ".7z", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".pdf"
    );

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----"),
            Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
            Pattern.compile("\\b(?:sk|stac)_[A-Za-z0-9_-]{16,}\\b"),
            Pattern.compile("\\bsk-[A-Za-z0-9_-]{20,}\\b"),
            Pattern.compile("(?i)\\b(?:CRJ_API_KEY|STAC_API_KEY|AWS_SECRET_ACCESS_KEY)\\s*=\\s*\\S+")
    );

    private KnowledgeIngestionPolicy() {
    }

    public static String normalizeAndValidatePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho da fonte obrigatorio.");
        }
        String trimmed = rawPath.trim().replace('\\', '/');
        if (trimmed.indexOf('\0') >= 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho da fonte invalido.");
        }
        if (trimmed.startsWith("/") || trimmed.matches("^[A-Za-z]:/.*")) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho da fonte deve ser relativo ao projeto.");
        }
        if (trimmed.length() > MAX_PATH_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho da fonte excede o limite permitido.");
        }
        String[] segments = trimmed.split("/");
        StringBuilder normalized = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new DomainException(ErrorCode.VALIDATION_ERROR, "Path traversal nao permitido.");
            }
            String lower = segment.toLowerCase(Locale.ROOT);
            if (FORBIDDEN_SEGMENTS.contains(lower) || isSensitiveFileName(lower)) {
                throw new DomainException(
                        ErrorCode.VALIDATION_ERROR,
                        "Arquivo ou diretorio sensivel bloqueado para indexacao."
                );
            }
            if (!normalized.isEmpty()) {
                normalized.append('/');
            }
            normalized.append(segment);
        }
        if (normalized.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Caminho da fonte invalido.");
        }
        String result = normalized.toString();
        String lowerResult = result.toLowerCase(Locale.ROOT);
        for (String ext : FORBIDDEN_EXTENSIONS) {
            if (lowerResult.endsWith(ext)) {
                throw new DomainException(
                        ErrorCode.VALIDATION_ERROR,
                        "Tipo de arquivo nao permitido para indexacao RAG."
                );
            }
        }
        return result;
    }

    public static String validateAndNormalizeContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo da fonte obrigatorio.");
        }
        if (rawContent.indexOf('\0') >= 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo binario nao permitido.");
        }
        String normalized = rawContent.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Conteudo da fonte obrigatorio.");
        }
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new DomainException(
                    ErrorCode.VALIDATION_ERROR,
                    "Conteudo da fonte excede o limite de " + MAX_CONTENT_LENGTH + " caracteres."
            );
        }
        for (Pattern pattern : SECRET_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                throw new DomainException(
                        ErrorCode.VALIDATION_ERROR,
                        "Possivel segredo ou credencial detectado no conteudo; indexacao bloqueada."
                );
            }
        }
        return normalized;
    }

    private static boolean isSensitiveFileName(String lowerSegment) {
        return ".env".equals(lowerSegment)
                || lowerSegment.startsWith(".env.")
                || lowerSegment.endsWith(".env")
                || lowerSegment.startsWith("id_rsa")
                || lowerSegment.startsWith("id_ed25519")
                || lowerSegment.contains("credentials")
                || lowerSegment.contains("secrets");
    }
}
