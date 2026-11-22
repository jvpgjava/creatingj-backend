package io.github.jvpgjava.creatingj.inference.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;

public interface ConcurrencyLimitPort {

    boolean tryAcquire(UserId userId, int maxConcurrent);

    void release(UserId userId);
}
