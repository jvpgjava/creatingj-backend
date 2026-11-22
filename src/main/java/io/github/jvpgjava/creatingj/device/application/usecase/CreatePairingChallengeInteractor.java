package io.github.jvpgjava.creatingj.device.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.in.CreatePairingChallengeUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.PairingChallengeStorePort;
import io.github.jvpgjava.creatingj.device.domain.PairingChallenge;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class CreatePairingChallengeInteractor implements CreatePairingChallengeUseCase {

    private static final Duration PAIRING_TTL = Duration.ofMinutes(5);

    private final PairingChallengeStorePort challengeStore;
    private final OpaqueTokenGeneratorPort tokenGenerator;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public CreatePairingChallengeInteractor(
            PairingChallengeStorePort challengeStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.challengeStore = challengeStore;
        this.tokenGenerator = tokenGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        if (command == null || command.actorUserId() == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        if (command.platform() == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Plataforma do dispositivo obrigatoria.");
        }
        Instant now = clock.now();
        Instant expiresAt = now.plus(PAIRING_TTL);
        String rawCode = tokenGenerator.generateToken();
        PairingChallenge challenge = challengeStore.save(new PairingChallenge(
                UUID.randomUUID(),
                command.actorUserId(),
                command.deviceName(),
                command.platform(),
                tokenGenerator.hash(rawCode),
                expiresAt,
                null,
                now
        ));
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.DEVICE_PAIRING_CREATED,
                "SUCCESS",
                "challengeId=" + challenge.id() + " platform=" + challenge.platform(),
                now
        ));
        return new Result(challenge.id(), rawCode, challenge.deviceName(), challenge.platform(), expiresAt);
    }
}
