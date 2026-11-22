package io.github.jvpgjava.creatingj.common.domain;

import java.time.Instant;

public record SystemStatus(
        String service,
        String apiVersion,
        String status,
        Instant timestamp
) {

    public SystemStatus {
        requireText(service, "service");
        requireText(apiVersion, "apiVersion");
        requireText(status, "status");
        if (timestamp == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "timestamp obrigatorio");
        }
    }

    public static SystemStatus up(String service, String apiVersion, Instant timestamp) {
        return new SystemStatus(service, apiVersion, "UP", timestamp);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, field + " obrigatorio");
        }
    }
}
