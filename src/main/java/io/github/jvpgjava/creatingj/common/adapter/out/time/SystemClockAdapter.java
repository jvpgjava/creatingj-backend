package io.github.jvpgjava.creatingj.common.adapter.out.time;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public final class SystemClockAdapter implements ClockPort {

    private final Clock clock;

    public SystemClockAdapter() {
        this.clock = Clock.systemUTC();
    }

    @Override
    public Instant now() {
        return Instant.now(clock);
    }
}
