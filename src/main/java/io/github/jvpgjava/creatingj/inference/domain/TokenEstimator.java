package io.github.jvpgjava.creatingj.inference.domain;

public final class TokenEstimator {

    private static final int CHARS_PER_TOKEN = 4;

    private TokenEstimator() {
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return estimateFromChars(text.length());
    }

    public static int estimateFromChars(int charCount) {
        if (charCount <= 0) {
            return 0;
        }
        return (charCount + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN;
    }
}
