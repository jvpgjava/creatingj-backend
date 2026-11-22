package io.github.jvpgjava.creatingj.device.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.in.CompleteDevicePairingUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.PairingChallengeStorePort;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceCredential;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.device.domain.DeviceStatus;
import io.github.jvpgjava.creatingj.device.domain.PairingChallenge;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;

import java.time.Instant;
import java.util.UUID;

public final class CompleteDevicePairingInteractor implements CompleteDevicePairingUseCase {

    private final PairingChallengeStorePort challengeStore;
    private final DeviceStorePort deviceStore;
    private final DeviceCredentialStorePort credentialStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public CompleteDevicePairingInteractor(
            PairingChallengeStorePort challengeStore,
            DeviceStorePort deviceStore,
            DeviceCredentialStorePort credentialStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.challengeStore = challengeStore;
        this.deviceStore = deviceStore;
        this.credentialStore = credentialStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        if (command == null || command.pairingCode() == null || command.pairingCode().isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Codigo de pareamento invalido ou expirado.");
        }
        Instant now = clock.now();
        String codeHash = tokenGenerator.hash(command.pairingCode().trim());
        PairingChallenge challenge = challengeStore.findByCodeHash(codeHash)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.UNAUTHORIZED,
                        "Codigo de pareamento invalido ou expirado."
                ));
        PairingChallenge consumed = challenge.consume(now);
        challengeStore.save(consumed);

        Device device = deviceStore.save(new Device(
                DeviceId.random(),
                consumed.ownerUserId(),
                consumed.deviceName(),
                consumed.platform(),
                DeviceStatus.ACTIVE,
                now,
                now,
                null
        ));
        String rawDeviceToken = tokenGenerator.generateToken();
        credentialStore.save(new DeviceCredential(
                UUID.randomUUID(),
                device.id(),
                device.ownerUserId(),
                tokenGenerator.hash(rawDeviceToken),
                now,
                null
        ));
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                device.ownerUserId().value(),
                device.ownerUserId().value(),
                AuditAction.DEVICE_PAIRED,
                "SUCCESS",
                "deviceId=" + device.id() + " platform=" + device.platform(),
                now
        ));
        return new Result(device, rawDeviceToken);
    }
}
