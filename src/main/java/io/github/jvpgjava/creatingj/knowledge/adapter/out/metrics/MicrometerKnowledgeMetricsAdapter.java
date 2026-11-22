package io.github.jvpgjava.creatingj.knowledge.adapter.out.metrics;

import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

public final class MicrometerKnowledgeMetricsAdapter implements KnowledgeMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerKnowledgeMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordIngestion(String status, int chunksIndexed, long durationMs) {
        Counter.builder("creatingj.knowledge.ingestions.total")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
        if (chunksIndexed > 0) {
            Counter.builder("creatingj.knowledge.chunks.indexed.total")
                    .register(meterRegistry)
                    .increment(chunksIndexed);
        }
        Timer.builder("creatingj.knowledge.ingestion.latency")
                .tag("status", status)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0L, durationMs)));
    }

    @Override
    public void recordSearch(int matchCount, long durationMs) {
        Counter.builder("creatingj.knowledge.searches.total")
                .register(meterRegistry)
                .increment();
        Timer.builder("creatingj.knowledge.search.latency")
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0L, durationMs)));
    }

    @Override
    public void recordSourceDeletion() {
        Counter.builder("creatingj.knowledge.sources.deleted.total")
                .register(meterRegistry)
                .increment();
    }
}
