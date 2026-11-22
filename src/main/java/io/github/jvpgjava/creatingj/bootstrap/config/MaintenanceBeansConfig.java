package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.maintenance.application.port.in.ExecuteDataRetentionUseCase;
import io.github.jvpgjava.creatingj.maintenance.application.port.out.DataRetentionStorePort;
import io.github.jvpgjava.creatingj.maintenance.application.usecase.ExecuteDataRetentionInteractor;
import io.github.jvpgjava.creatingj.maintenance.domain.RetentionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaintenanceBeansConfig {

    @Bean
    public RetentionPolicy retentionPolicy(
            @Value("${creatingj.retention.expired-sessions-days:30}") int expiredSessionsDays,
            @Value("${creatingj.retention.expired-pairing-challenges-days:7}") int expiredPairingChallengesDays,
            @Value("${creatingj.retention.completed-tasks-days:90}") int completedTasksDays,
            @Value("${creatingj.retention.archived-conversations-days:90}") int archivedConversationsDays,
            @Value("${creatingj.retention.audit-events-days:180}") int auditEventsDays
    ) {
        return new RetentionPolicy(
                expiredSessionsDays,
                expiredPairingChallengesDays,
                completedTasksDays,
                archivedConversationsDays,
                auditEventsDays
        );
    }

    @Bean
    public ExecuteDataRetentionUseCase executeDataRetentionUseCase(
            DataRetentionStorePort retentionStore,
            RetentionPolicy retentionPolicy,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new ExecuteDataRetentionInteractor(retentionStore, retentionPolicy, auditPort, clock);
    }
}
