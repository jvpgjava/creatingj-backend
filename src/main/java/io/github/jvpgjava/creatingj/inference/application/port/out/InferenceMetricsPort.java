package io.github.jvpgjava.creatingj.inference.application.port.out;

import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;

public interface InferenceMetricsPort {

    void recordGeneration(
            String modelId,
            UsageSource source,
            UsageOutcome outcome,
            long latencyMs,
            int totalTokens
    );

    void recordQuotaRejected(String reason);
}
