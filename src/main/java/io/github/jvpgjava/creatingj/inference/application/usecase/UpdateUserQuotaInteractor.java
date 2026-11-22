package io.github.jvpgjava.creatingj.inference.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.inference.application.port.in.UpdateUserQuotaUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;

import java.time.Instant;
import java.util.UUID;

public final class UpdateUserQuotaInteractor implements UpdateUserQuotaUseCase {

    private final UserStorePort userStore;
    private final UserQuotaStorePort quotaStore;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public UpdateUserQuotaInteractor(
            UserStorePort userStore,
            UserQuotaStorePort quotaStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.userStore = userStore;
        this.quotaStore = quotaStore;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public UserQuota execute(UpdateUserQuotaCommand command) {
        User actor = userStore.findById(command.actorUserId())
                .filter(u -> u.isActive() && u.isAdmin())
                .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN, "Acesso administrativo negado."));

        userStore.findById(command.targetUserId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Usuario nao encontrado."));

        Instant now = clock.now();
        UserQuota quota = UserQuota.of(
                command.targetUserId(),
                command.dailyTokenLimit(),
                command.dailyRequestLimit(),
                command.maxConcurrentGenerations(),
                actor.id(),
                now
        );
        UserQuota saved = quotaStore.save(quota);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                actor.id().value(),
                command.targetUserId().value(),
                AuditAction.USER_QUOTA_UPDATED,
                "SUCCESS",
                "Quota do usuario atualizada pelo administrador.",
                now
        ));

        return saved;
    }
}
