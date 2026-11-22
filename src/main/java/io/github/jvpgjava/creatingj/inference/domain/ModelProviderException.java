package io.github.jvpgjava.creatingj.inference.domain;

import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

public final class ModelProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ProviderFailureKind failureKind;

    public ModelProviderException(ProviderFailureKind failureKind, String message) {
        this(failureKind, message, null);
    }

    public ModelProviderException(ProviderFailureKind failureKind, String message, Throwable cause) {
        super(message, cause);
        this.failureKind = failureKind;
    }

    public ProviderFailureKind failureKind() {
        return failureKind;
    }

    public ErrorCode toErrorCode() {
        return switch (failureKind) {
            case RATE_LIMITED -> ErrorCode.PROVIDER_RATE_LIMITED;
            case TIMEOUT -> ErrorCode.PROVIDER_TIMEOUT;
            case UNAVAILABLE -> ErrorCode.PROVIDER_UNAVAILABLE;
            case UNAUTHORIZED, BAD_RESPONSE -> ErrorCode.BAD_GATEWAY;
            case CANCELLED -> ErrorCode.INVALID_STATE;
        };
    }
}
