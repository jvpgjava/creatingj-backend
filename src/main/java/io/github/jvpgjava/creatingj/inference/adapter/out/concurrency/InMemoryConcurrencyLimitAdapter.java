package io.github.jvpgjava.creatingj.inference.adapter.out.concurrency;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.out.ConcurrencyLimitPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public final class InMemoryConcurrencyLimitAdapter implements ConcurrencyLimitPort {

    private final Map<UserId, Integer> activeByUser = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(UserId userId, int maxConcurrent) {
        AtomicBoolean acquired = new AtomicBoolean(false);
        activeByUser.compute(userId, (k, existing) -> {
            int current = existing == null ? 0 : existing;
            if (current >= maxConcurrent) {
                return current;
            }
            acquired.set(true);
            return current + 1;
        });
        return acquired.get();
    }

    @Override
    public void release(UserId userId) {
        activeByUser.computeIfPresent(userId, (k, existing) -> existing <= 1 ? null : existing - 1);
    }
}
