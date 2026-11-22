package io.github.jvpgjava.creatingj.inference.adapter.out.metrics;

import io.github.jvpgjava.creatingj.inference.application.port.out.InferenceMetricsPort;
import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public final class MicrometerInferenceMetricsAdapter implements InferenceMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerInferenceMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordGeneration(
            String modelId,
            UsageSource source,
            UsageOutcome outcome,
            long latencyMs,
            int totalTokens
    ) {
        Counter.builder("creatingj.inference.requests")
                .tag("model", modelId)
                .tag("source", source.name())
                .tag("outcome", outcome.name())
                .register(meterRegistry)
                .increment();

        if (totalTokens > 0) {
            Counter.builder("creatingj.inference.tokens")
                    .tag("model", modelId)
                    .tag("source", source.name())
                    .register(meterRegistry)
                    .increment(totalTokens);
        }

        Timer.builder("creatingj.inference.latency")
                .tag("model", modelId)
                .tag("source", source.name())
                .tag("outcome", outcome.name())
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordQuotaRejected(String reason) {
        Counter.builder("creatingj.inference.quota.rejected")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}
