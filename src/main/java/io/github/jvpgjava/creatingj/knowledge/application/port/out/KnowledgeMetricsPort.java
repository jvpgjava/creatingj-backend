package io.github.jvpgjava.creatingj.knowledge.application.port.out;

public interface KnowledgeMetricsPort {

    void recordIngestion(String status, int chunksIndexed, long durationMs);

    void recordSearch(int matchCount, long durationMs);

    void recordSourceDeletion();
}
