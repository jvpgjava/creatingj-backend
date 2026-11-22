package io.github.jvpgjava.creatingj.inference.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;

public record UserQuota(
        UserId userId,
        long dailyTokenLimit,
        int dailyRequestLimit,
        int maxConcurrentGenerations,
        UserId updatedBy,
        Instant updatedAt
) {

    private static final long MAX_DAILY_TOKENS = 100_000_000L;
    private static final int MAX_DAILY_REQUESTS = 100_000;
    private static final int MAX_CONCURRENT = 50;

    public UserQuota {
        if (userId == null || updatedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de quota ausentes.");
        }
        if (dailyTokenLimit < 1 || dailyTokenLimit > MAX_DAILY_TOKENS) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "dailyTokenLimit fora do intervalo permitido.");
        }
        if (dailyRequestLimit < 1 || dailyRequestLimit > MAX_DAILY_REQUESTS) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "dailyRequestLimit fora do intervalo permitido.");
        }
        if (maxConcurrentGenerations < 1 || maxConcurrentGenerations > MAX_CONCURRENT) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "maxConcurrentGenerations deve estar entre 1 e 50.");
        }
    }

    public static UserQuota of(
            UserId userId,
            long dailyTokenLimit,
            int dailyRequestLimit,
            int maxConcurrentGenerations,
            UserId updatedBy,
            Instant updatedAt
    ) {
        return new UserQuota(userId, dailyTokenLimit, dailyRequestLimit, maxConcurrentGenerations, updatedBy,
                updatedAt);
    }
}
