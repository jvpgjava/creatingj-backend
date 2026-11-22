package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.device.adapter.in.web.mapper.v1.DeviceHttpMapper;
import io.github.jvpgjava.creatingj.device.application.port.in.CompleteDevicePairingUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.CreatePairingChallengeUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.ListUserDevicesUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.RecordDeviceHeartbeatUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.ResolveAuthenticatedDeviceUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.RevokeDeviceUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.PairingChallengeStorePort;
import io.github.jvpgjava.creatingj.device.application.usecase.CompleteDevicePairingInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.CreatePairingChallengeInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.ListUserDevicesInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.RecordDeviceHeartbeatInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.ResolveAuthenticatedDeviceInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.RevokeDeviceInteractor;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceBeansConfig {

    @Bean
    public CreatePairingChallengeUseCase createPairingChallengeUseCase(
            PairingChallengeStorePort challengeStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new CreatePairingChallengeInteractor(challengeStore, tokenGenerator, auditPort, clock);
    }

    @Bean
    public CompleteDevicePairingUseCase completeDevicePairingUseCase(
            PairingChallengeStorePort challengeStore,
            DeviceStorePort deviceStore,
            DeviceCredentialStorePort credentialStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new CompleteDevicePairingInteractor(
                challengeStore, deviceStore, credentialStore, tokenGenerator, auditPort, clock);
    }

    @Bean
    public ListUserDevicesUseCase listUserDevicesUseCase(DeviceStorePort deviceStore, ClockPort clock) {
        return new ListUserDevicesInteractor(deviceStore, clock);
    }

    @Bean
    public RevokeDeviceUseCase revokeDeviceUseCase(
            DeviceStorePort deviceStore,
            DeviceCredentialStorePort credentialStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new RevokeDeviceInteractor(deviceStore, credentialStore, auditPort, clock);
    }

    @Bean
    public RecordDeviceHeartbeatUseCase recordDeviceHeartbeatUseCase(DeviceStorePort deviceStore, ClockPort clock) {
        return new RecordDeviceHeartbeatInteractor(deviceStore, clock);
    }

    @Bean
    public ResolveAuthenticatedDeviceUseCase resolveAuthenticatedDeviceUseCase(
            DeviceCredentialStorePort credentialStore,
            DeviceStorePort deviceStore,
            OpaqueTokenGeneratorPort tokenGenerator
    ) {
        return new ResolveAuthenticatedDeviceInteractor(credentialStore, deviceStore, tokenGenerator);
    }

    @Bean
    public DeviceHttpMapper deviceHttpMapper() {
        return new DeviceHttpMapper();
    }
}
