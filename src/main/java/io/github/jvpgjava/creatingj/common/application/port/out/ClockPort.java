package io.github.jvpgjava.creatingj.common.application.port.out;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
