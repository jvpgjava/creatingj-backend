package io.github.jvpgjava.creatingj.inference.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;

import java.util.UUID;

public interface EnforceQuotaAndRecordUsageUseCase {

    void acquirePermit(UserId userId);

    UsageRecord recordAndRelease(RecordUsageCommand command);

    record RecordUsageCommand(
            UserId userId,
            UUID conversationId,
            UUID generationId,
            String modelId,
            UsageSource source,
            TokenUsage reportedUsage,
            int promptCharCount,
            int completionCharCount,
            long latencyMs,
            UsageOutcome outcome
    ) {
    }
}
