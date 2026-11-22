package io.github.jvpgjava.creatingj.maintenance.adapter.in.web.dto.v1;

import java.time.Instant;

public record DataRetentionReportResponse(
        Instant executedAt,
        int deletedSessions,
        int deletedPairingChallenges,
        int deletedCompletedTasks,
        int deletedArchivedConversations,
        int deletedAuditEvents,
        int totalDeleted
) {
}
