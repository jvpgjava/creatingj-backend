package io.github.jvpgjava.creatingj.inference.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

record UtcDayWindow(Instant startInclusive, Instant endExclusive) {

    static UtcDayWindow containing(Instant now) {
        LocalDate day = now.atOffset(ZoneOffset.UTC).toLocalDate();
        Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new UtcDayWindow(start, end);
    }
}
