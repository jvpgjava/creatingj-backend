package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

final class AdminAccessGuard {

    private AdminAccessGuard() {
    }

    static User requireActiveAdmin(UserStorePort userStore, UserId actorUserId) {
        if (actorUserId == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        User actor = userStore.findById(actorUserId)
                .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN, "Acesso administrativo negado."));
        if (!actor.isActive() || !actor.isAdmin()) {
            throw new DomainException(ErrorCode.FORBIDDEN, "Acesso administrativo negado.");
        }
        return actor;
    }
}
