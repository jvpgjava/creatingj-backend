package io.github.jvpgjava.creatingj.maintenance.domain;

import java.time.Instant;

public record DataRetentionReport(
        Instant executedAt,
        int deletedSessions,
        int deletedPairingChallenges,
        int deletedCompletedTasks,
        int deletedArchivedConversations,
        int deletedAuditEvents
) {

    public int totalDeleted() {
        return deletedSessions
                + deletedPairingChallenges
                + deletedCompletedTasks
                + deletedArchivedConversations
                + deletedAuditEvents;
    }
}
