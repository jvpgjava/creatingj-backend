package io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateUserQuotaRequest(
        @Min(1) @Max(100_000_000L) long dailyTokenLimit,
        @Min(1) @Max(100_000) int dailyRequestLimit,
        @Min(1) @Max(50) int maxConcurrentGenerations
) {
}
