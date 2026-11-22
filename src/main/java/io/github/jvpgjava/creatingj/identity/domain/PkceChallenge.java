package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

public record PkceChallenge(String value, String method) {

    public static final String METHOD_S256 = "S256";

    private static final Pattern BASE64URL_TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43,128}$");
    private static final Pattern VERIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9._~-]{43,128}$");

    public PkceChallenge {
        if (!METHOD_S256.equals(method)) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Apenas code_challenge_method=S256 e suportado.");
        }
        if (value == null || !BASE64URL_TOKEN.matcher(value).matches()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "code_challenge invalido.");
        }
    }

    public static PkceChallenge s256(String value, String method) {
        return new PkceChallenge(value, method);
    }

    public boolean matchesVerifier(String codeVerifier) {
        if (codeVerifier == null || !VERIFIER_PATTERN.matcher(codeVerifier).matches()) {
            return false;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    value.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM.", ex);
        }
    }
}
