package io.github.jvpgjava.creatingj.identity.application.port.out;

public interface LoginAttemptGuardPort {

    boolean isBlocked(String key);

    void recordFailure(String key);

    void reset(String key);
}
