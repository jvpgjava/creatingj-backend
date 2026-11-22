package io.github.jvpgjava.creatingj.device.application.usecase;

import io.github.jvpgjava.creatingj.device.application.port.in.ResolveAuthenticatedDeviceUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.domain.AuthenticatedDevicePrincipal;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;

import java.util.Optional;

public final class ResolveAuthenticatedDeviceInteractor implements ResolveAuthenticatedDeviceUseCase {

    private final DeviceCredentialStorePort credentialStore;
    private final DeviceStorePort deviceStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;

    public ResolveAuthenticatedDeviceInteractor(
            DeviceCredentialStorePort credentialStore,
            DeviceStorePort deviceStore,
            OpaqueTokenGeneratorPort tokenGenerator
    ) {
        this.credentialStore = credentialStore;
        this.deviceStore = deviceStore;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Optional<AuthenticatedDevicePrincipal> execute(String rawDeviceToken) {
        if (rawDeviceToken == null || rawDeviceToken.isBlank()) {
            return Optional.empty();
        }
        String hash = tokenGenerator.hash(rawDeviceToken.trim());
        return credentialStore.findByTokenHash(hash)
                .filter(c -> c.isActive())
                .flatMap(credential -> deviceStore.findById(credential.deviceId())
                        .filter(Device::isActive)
                        .map(device -> new AuthenticatedDevicePrincipal(
                                device.id(),
                                device.ownerUserId(),
                                credential.id()
                        )));
    }
}
