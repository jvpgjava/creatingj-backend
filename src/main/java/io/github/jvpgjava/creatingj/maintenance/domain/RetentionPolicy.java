package io.github.jvpgjava.creatingj.maintenance.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

public record RetentionPolicy(
        int expiredSessionsDays,
        int expiredPairingChallengesDays,
        int completedTasksDays,
        int archivedConversationsDays,
        int auditEventsDays
) {

    public RetentionPolicy {
        if (expiredSessionsDays < 1 || expiredPairingChallengesDays < 1
                || completedTasksDays < 1 || archivedConversationsDays < 1 || auditEventsDays < 7) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Janela de retencao invalida.");
        }
    }

    public static RetentionPolicy defaults() {
        return new RetentionPolicy(30, 7, 90, 90, 180);
    }
}
