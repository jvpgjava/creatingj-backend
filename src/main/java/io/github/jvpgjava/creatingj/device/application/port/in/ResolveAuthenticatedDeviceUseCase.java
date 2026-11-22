package io.github.jvpgjava.creatingj.device.application.port.in;

import io.github.jvpgjava.creatingj.device.domain.AuthenticatedDevicePrincipal;

import java.util.Optional;

public interface ResolveAuthenticatedDeviceUseCase {

    Optional<AuthenticatedDevicePrincipal> execute(String rawDeviceToken);
}
