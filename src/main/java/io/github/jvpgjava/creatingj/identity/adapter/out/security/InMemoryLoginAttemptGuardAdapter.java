package io.github.jvpgjava.creatingj.identity.adapter.out.security;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.LoginAttemptGuardPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLoginAttemptGuardAdapter implements LoginAttemptGuardPort {

    private final ClockPort clock;
    private final int maxAttempts;
    private final Duration window;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public InMemoryLoginAttemptGuardAdapter(ClockPort clock, int maxAttempts, Duration window) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    @Override
    public boolean isBlocked(String key) {
        Instant now = clock.now();
        AttemptWindow state = attempts.get(key);
        if (state == null) {
            return false;
        }
        if (!state.windowEndsAt().isAfter(now)) {
            attempts.remove(key, state);
            return false;
        }
        return state.failures() >= maxAttempts;
    }

    @Override
    public void recordFailure(String key) {
        Instant now = clock.now();
        attempts.compute(key, (k, existing) -> {
            if (existing == null || !existing.windowEndsAt().isAfter(now)) {
                return new AttemptWindow(1, now.plus(window));
            }
            return new AttemptWindow(existing.failures() + 1, existing.windowEndsAt());
        });
    }

    @Override
    public void reset(String key) {
        attempts.remove(key);
    }

    private record AttemptWindow(int failures, Instant windowEndsAt) {
    }
}
