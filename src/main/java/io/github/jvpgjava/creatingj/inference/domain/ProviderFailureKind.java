package io.github.jvpgjava.creatingj.inference.domain;

public enum ProviderFailureKind {
    UNAUTHORIZED,
    RATE_LIMITED,
    TIMEOUT,
    UNAVAILABLE,
    BAD_RESPONSE,
    CANCELLED
}
