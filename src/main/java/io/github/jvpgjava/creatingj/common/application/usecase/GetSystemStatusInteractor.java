package io.github.jvpgjava.creatingj.common.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.in.GetSystemStatusUseCase;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.SystemStatus;

public final class GetSystemStatusInteractor implements GetSystemStatusUseCase {

    private static final String SERVICE_NAME = "creatingj-backend";
    private static final String API_VERSION = "v1";

    private final ClockPort clockPort;

    public GetSystemStatusInteractor(ClockPort clockPort) {
        if (clockPort == null) {
            throw new IllegalArgumentException("clockPort must not be null");
        }
        this.clockPort = clockPort;
    }

    @Override
    public SystemStatus execute() {
        return SystemStatus.up(SERVICE_NAME, API_VERSION, clockPort.now());
    }
}
