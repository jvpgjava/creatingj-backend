package io.github.jvpgjava.creatingj.maintenance.application.port.out;

import java.time.Instant;

public interface DataRetentionStorePort {

    int deleteExpiredOrRevokedSessionsBefore(Instant cutoff);

    int deleteExpiredPairingChallengesBefore(Instant cutoff);

    int deleteTerminalTasksBefore(Instant cutoff);

    int deleteArchivedConversationsBefore(Instant cutoff);

    int deleteAuditEventsBefore(Instant cutoff);
}
