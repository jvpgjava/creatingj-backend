package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Email obrigatorio.");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Email invalido.");
        }
        value = normalized;
    }

    public static Email of(String raw) {
        return new Email(raw);
    }
}
