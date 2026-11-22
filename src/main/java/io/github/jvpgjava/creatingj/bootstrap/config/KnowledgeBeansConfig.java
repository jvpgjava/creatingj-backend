package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.bootstrap.config.provider.EmbeddingProperties;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.mapper.v1.KnowledgeHttpMapper;
import io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding.DeterministicHashedEmbeddingAdapter;
import io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding.OpenAiCompatibleEmbeddingAdapter;
import io.github.jvpgjava.creatingj.knowledge.adapter.out.metrics.MicrometerKnowledgeMetricsAdapter;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.DeleteKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.IngestKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeIndexJobsUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeSourcesUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.SearchProjectKnowledgeUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.EmbeddingPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeIndexJobStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeMetricsPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.application.usecase.DeleteKnowledgeSourceInteractor;
import io.github.jvpgjava.creatingj.knowledge.application.usecase.IngestKnowledgeSourceInteractor;
import io.github.jvpgjava.creatingj.knowledge.application.usecase.ListKnowledgeIndexJobsInteractor;
import io.github.jvpgjava.creatingj.knowledge.application.usecase.ListKnowledgeSourcesInteractor;
import io.github.jvpgjava.creatingj.knowledge.application.usecase.ProjectKnowledgeCleanupService;
import io.github.jvpgjava.creatingj.knowledge.application.usecase.SearchProjectKnowledgeInteractor;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectKnowledgeCleanupPort;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class KnowledgeBeansConfig {

    @Bean
    public EmbeddingPort embeddingPort(EmbeddingProperties properties, ObjectMapper objectMapper) {
        if ("openai-compatible".equals(properties.provider())) {
            return new OpenAiCompatibleEmbeddingAdapter(
                    properties.baseUrl(),
                    properties.apiKey(),
                    properties.model(),
                    Duration.ofSeconds(Math.max(1, properties.timeoutSeconds())),
                    objectMapper
            );
        }
        return new DeterministicHashedEmbeddingAdapter();
    }

    @Bean
    public KnowledgeMetricsPort knowledgeMetricsPort(MeterRegistry meterRegistry) {
        return new MicrometerKnowledgeMetricsAdapter(meterRegistry);
    }

    @Bean
    public ProjectKnowledgeCleanupPort projectKnowledgeCleanupPort(
            VectorKnowledgePort vectorStore,
            KnowledgeSourceStorePort sourceStore
    ) {
        return new ProjectKnowledgeCleanupService(vectorStore, sourceStore);
    }

    @Bean
    public IngestKnowledgeSourceUseCase ingestKnowledgeSourceUseCase(
            ProjectStorePort projectStore,
            KnowledgeSourceStorePort sourceStore,
            VectorKnowledgePort vectorStore,
            KnowledgeIndexJobStorePort jobStore,
            EmbeddingPort embeddingPort,
            KnowledgeMetricsPort metricsPort,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new IngestKnowledgeSourceInteractor(
                projectStore,
                sourceStore,
                vectorStore,
                jobStore,
                embeddingPort,
                metricsPort,
                auditPort,
                clock
        );
    }

    @Bean
    public ListKnowledgeSourcesUseCase listKnowledgeSourcesUseCase(
            ProjectStorePort projectStore,
            KnowledgeSourceStorePort sourceStore
    ) {
        return new ListKnowledgeSourcesInteractor(projectStore, sourceStore);
    }

    @Bean
    public DeleteKnowledgeSourceUseCase deleteKnowledgeSourceUseCase(
            ProjectStorePort projectStore,
            KnowledgeSourceStorePort sourceStore,
            VectorKnowledgePort vectorStore,
            KnowledgeIndexJobStorePort jobStore,
            KnowledgeMetricsPort metricsPort,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new DeleteKnowledgeSourceInteractor(
                projectStore,
                sourceStore,
                vectorStore,
                jobStore,
                metricsPort,
                auditPort,
                clock
        );
    }

    @Bean
    public SearchProjectKnowledgeUseCase searchProjectKnowledgeUseCase(
            ProjectStorePort projectStore,
            EmbeddingPort embeddingPort,
            VectorKnowledgePort vectorStore,
            KnowledgeMetricsPort metricsPort
    ) {
        return new SearchProjectKnowledgeInteractor(projectStore, embeddingPort, vectorStore, metricsPort);
    }

    @Bean
    public ListKnowledgeIndexJobsUseCase listKnowledgeIndexJobsUseCase(
            ProjectStorePort projectStore,
            KnowledgeIndexJobStorePort jobStore
    ) {
        return new ListKnowledgeIndexJobsInteractor(projectStore, jobStore);
    }

    @Bean
    public KnowledgeHttpMapper knowledgeHttpMapper() {
        return new KnowledgeHttpMapper();
    }
}
