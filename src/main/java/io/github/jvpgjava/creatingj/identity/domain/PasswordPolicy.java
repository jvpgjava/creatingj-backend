package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

public final class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "A senha deve conter entre 12 e 128 caracteres.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (int i = 0; i < rawPassword.length(); i++) {
            char c = rawPassword.charAt(i);
            if (Character.isISOControl(c)) {
                throw new DomainException(ErrorCode.VALIDATION_ERROR,
                        "A senha contem caracteres invalidos.");
            }
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }

        if (!hasUpper || !hasLower || !hasDigit || !hasSymbol) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "A senha deve incluir maiuscula, minuscula, numero e simbolo.");
        }
    }
}
