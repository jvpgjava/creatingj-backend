package io.github.jvpgjava.creatingj.device.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.in.RevokeDeviceUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.domain.Device;

import java.time.Instant;
import java.util.UUID;

public final class RevokeDeviceInteractor implements RevokeDeviceUseCase {

    private final DeviceStorePort deviceStore;
    private final DeviceCredentialStorePort credentialStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public RevokeDeviceInteractor(
            DeviceStorePort deviceStore,
            DeviceCredentialStorePort credentialStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.deviceStore = deviceStore;
        this.credentialStore = credentialStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Device execute(Command command) {
        if (command == null || command.actorUserId() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        Device device = deviceStore.findById(command.deviceId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Dispositivo nao encontrado."));
        if (!device.isOwnedBy(command.actorUserId())) {
            throw new DomainException(ErrorCode.NOT_FOUND, "Dispositivo nao encontrado.");
        }
        Instant now = clock.now();
        Device revoked = deviceStore.save(device.revoke(now));
        credentialStore.revokeAllByDeviceId(device.id(), now);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.DEVICE_REVOKED,
                "SUCCESS",
                "deviceId=" + device.id(),
                now
        ));
        return revoked;
    }
}
