package io.github.jvpgjava.creatingj.knowledge;

import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding.DeterministicHashedEmbeddingAdapter;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.DeleteKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.IngestKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeIndexJobsUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeSourcesUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.SearchProjectKnowledgeUseCase;
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
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunk;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeJobStatus;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:10:00Z");

    @Test
    void shouldIngestDeduplicateSearchIsolateAndDeleteKnowledgeSources() {
        Map<ProjectId, Project> projects = new ConcurrentHashMap<>();
        ProjectStorePort projectStore = fakeProjectStore(projects);
        InMemorySourceStore sourceStore = new InMemorySourceStore();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore(sourceStore);
        InMemoryJobStore jobStore = new InMemoryJobStore();
        DeterministicHashedEmbeddingAdapter embedder = new DeterministicHashedEmbeddingAdapter();
        List<AuditEvent> audits = new ArrayList<>();
        ClockPort clock = () -> NOW;
        KnowledgeMetricsPort metrics = noopMetrics();

        UserId userA = UserId.newId();
        UserId userB = UserId.newId();
        Project projectA = Project.create(ProjectId.random(), userA, "CreatingJ", "creatingj", "Backend", NOW);
        Project projectB = Project.create(ProjectId.random(), userB, "Other", "other", "Private", NOW);
        projects.put(projectA.id(), projectA);
        projects.put(projectB.id(), projectB);

        IngestKnowledgeSourceInteractor ingest = new IngestKnowledgeSourceInteractor(
                projectStore, sourceStore, vectorStore, jobStore, embedder, metrics, audits::add, clock);
        ListKnowledgeSourcesInteractor listSources = new ListKnowledgeSourcesInteractor(projectStore, sourceStore);
        DeleteKnowledgeSourceInteractor deleteSource = new DeleteKnowledgeSourceInteractor(
                projectStore, sourceStore, vectorStore, jobStore, metrics, audits::add, clock);
        SearchProjectKnowledgeInteractor search = new SearchProjectKnowledgeInteractor(
                projectStore, embedder, vectorStore, metrics);
        ListKnowledgeIndexJobsInteractor listJobs = new ListKnowledgeIndexJobsInteractor(projectStore, jobStore);

        var first = ingest.execute(new IngestKnowledgeSourceUseCase.Command(
                userA, projectA.id(), "docs/pgvector.md", "Guia pgvector",
                "PostgreSQL 18.6 com extensao pgvector e indice HNSW de cosseno para busca RAG."
        ));
        assertThat(first.deduplicated()).isFalse();
        assertThat(first.source().chunkCount()).isEqualTo(1);
        assertThat(first.job().status()).isEqualTo(KnowledgeJobStatus.COMPLETED);

        var secondSame = ingest.execute(new IngestKnowledgeSourceUseCase.Command(
                userA, projectA.id(), "docs/pgvector.md", "Guia pgvector",
                "PostgreSQL 18.6 com extensao pgvector e indice HNSW de cosseno para busca RAG."
        ));
        assertThat(secondSame.deduplicated()).isTrue();
        assertThat(secondSame.job().status()).isEqualTo(KnowledgeJobStatus.SKIPPED_UNCHANGED);

        ingest.execute(new IngestKnowledgeSourceUseCase.Command(
                userB, projectB.id(), "docs/secret-b.md", "Segredo B",
                "Documento confidencial exclusivo do projeto B com pgvector HNSW."
        ));

        var searchResult = search.execute(new SearchProjectKnowledgeUseCase.Query(
                userA, projectA.id(), "indice HNSW pgvector PostgreSQL", 5, 0.0));
        assertThat(searchResult.matches()).hasSize(1);
        assertThat(searchResult.matches().get(0).sourcePath()).isEqualTo("docs/pgvector.md");

        assertThatThrownBy(() -> ingest.execute(new IngestKnowledgeSourceUseCase.Command(
                userA, projectA.id(), ".env", "Env", "CRJ_API_KEY=leaked-value")))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(audits.stream().anyMatch(e -> e.action() == AuditAction.KNOWLEDGE_INGESTION_REJECTED)).isTrue();

        assertThatThrownBy(() -> search.execute(new SearchProjectKnowledgeUseCase.Query(
                userB, projectA.id(), "pgvector", 5, 0.0)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> listSources.execute(new ListKnowledgeSourcesUseCase.Query(userB, projectA.id())))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> deleteSource.execute(
                new DeleteKnowledgeSourceUseCase.Command(userB, projectA.id(), first.source().id())))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> listJobs.execute(new ListKnowledgeIndexJobsUseCase.Query(userB, projectA.id(), 10)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);

        assertThat(listSources.execute(new ListKnowledgeSourcesUseCase.Query(userA, projectA.id()))).hasSize(1);
        assertThat(listJobs.execute(new ListKnowledgeIndexJobsUseCase.Query(userA, projectA.id(), 0))).hasSize(2);

        deleteSource.execute(new DeleteKnowledgeSourceUseCase.Command(userA, projectA.id(), first.source().id()));
        assertThat(listSources.execute(new ListKnowledgeSourcesUseCase.Query(userA, projectA.id()))).isEmpty();
        assertThat(search.execute(new SearchProjectKnowledgeUseCase.Query(
                userA, projectA.id(), "pgvector", 5, 0.0)).matches()).isEmpty();

        ProjectKnowledgeCleanupService cleanup = new ProjectKnowledgeCleanupService(vectorStore, sourceStore);
        cleanup.deleteAllForProject(projectB.id(), userB);
        assertThat(listSources.execute(new ListKnowledgeSourcesUseCase.Query(userB, projectB.id()))).isEmpty();

        assertThatThrownBy(() -> search.execute(new SearchProjectKnowledgeUseCase.Query(
                userA, projectA.id(), " ", 5, 0.0)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private static KnowledgeMetricsPort noopMetrics() {
        return new KnowledgeMetricsPort() {
            @Override
            public void recordIngestion(String status, int chunksIndexed, long durationMs) {
            }

            @Override
            public void recordSearch(int matchCount, long durationMs) {
            }

            @Override
            public void recordSourceDeletion() {
            }
        };
    }

    private static ProjectStorePort fakeProjectStore(Map<ProjectId, Project> projects) {
        return new ProjectStorePort() {
            @Override
            public Project save(Project project) {
                projects.put(project.id(), project);
                return project;
            }

            @Override
            public Optional<Project> findById(ProjectId projectId) {
                return Optional.ofNullable(projects.get(projectId));
            }

            @Override
            public Optional<Project> findByOwnerAndSlug(UserId ownerUserId, String slug) {
                return Optional.empty();
            }

            @Override
            public List<Project> findByOwner(UserId ownerUserId, int offset, int limit) {
                return List.of();
            }

            @Override
            public long countByOwner(UserId ownerUserId) {
                return 0;
            }

            @Override
            public void deleteById(ProjectId projectId) {
                projects.remove(projectId);
            }
        };
    }

    private static final class InMemorySourceStore implements KnowledgeSourceStorePort {

        private final Map<KnowledgeSourceId, KnowledgeSource> store = new ConcurrentHashMap<>();

        @Override
        public KnowledgeSource save(KnowledgeSource source) {
            store.put(source.id(), source);
            return source;
        }

        @Override
        public Optional<KnowledgeSource> findById(KnowledgeSourceId sourceId) {
            return Optional.ofNullable(store.get(sourceId));
        }

        @Override
        public Optional<KnowledgeSource> findByProjectAndPath(ProjectId projectId, String sourcePath) {
            return store.values().stream()
                    .filter(s -> s.projectId().equals(projectId) && s.sourcePath().equals(sourcePath))
                    .findFirst();
        }

        @Override
        public List<KnowledgeSource> findByProjectAndOwner(ProjectId projectId, UserId ownerUserId) {
            return store.values().stream().filter(s -> s.belongsTo(projectId, ownerUserId)).toList();
        }

        @Override
        public void deleteById(KnowledgeSourceId sourceId) {
            store.remove(sourceId);
        }

        @Override
        public void deleteByProject(ProjectId projectId, UserId ownerUserId) {
            store.entrySet().removeIf(e -> e.getValue().belongsTo(projectId, ownerUserId));
        }
    }

    private static final class InMemoryVectorStore implements VectorKnowledgePort {

        private final KnowledgeSourceStorePort sourceStore;
        private final List<KnowledgeChunk> chunks = new CopyOnWriteArrayList<>();

        private InMemoryVectorStore(KnowledgeSourceStorePort sourceStore) {
            this.sourceStore = sourceStore;
        }

        @Override
        public void replaceSourceChunks(KnowledgeSourceId sourceId, List<KnowledgeChunk> newChunks) {
            chunks.removeIf(c -> c.sourceId().equals(sourceId));
            chunks.addAll(newChunks);
        }

        @Override
        public List<KnowledgeSearchMatch> searchSimilar(
                ProjectId projectId,
                UserId ownerUserId,
                EmbeddingVector queryVector,
                String embeddingVersion,
                int topK,
                double minScore
        ) {
            return chunks.stream()
                    .filter(c -> c.projectId().equals(projectId)
                            && c.ownerUserId().equals(ownerUserId)
                            && c.embeddingVersion().equals(embeddingVersion))
                    .map(c -> {
                        KnowledgeSource source = sourceStore.findById(c.sourceId()).orElse(null);
                        if (source == null) {
                            return null;
                        }
                        return new KnowledgeSearchMatch(
                                c.sourceId(),
                                source.sourcePath(),
                                source.title(),
                                c.chunkIndex(),
                                c.content(),
                                c.embedding().cosineSimilarity(queryVector),
                                c.embeddingVersion()
                        );
                    })
                    .filter(m -> m != null && m.score() >= minScore)
                    .sorted(Comparator.comparingDouble(KnowledgeSearchMatch::score).reversed())
                    .limit(topK)
                    .toList();
        }

        @Override
        public void deleteBySource(KnowledgeSourceId sourceId) {
            chunks.removeIf(c -> c.sourceId().equals(sourceId));
        }

        @Override
        public void deleteByProject(ProjectId projectId, UserId ownerUserId) {
            chunks.removeIf(c -> c.projectId().equals(projectId) && c.ownerUserId().equals(ownerUserId));
        }
    }

    private static final class InMemoryJobStore implements KnowledgeIndexJobStorePort {

        private final List<KnowledgeIndexJob> jobs = new CopyOnWriteArrayList<>();

        @Override
        public KnowledgeIndexJob save(KnowledgeIndexJob job) {
            jobs.removeIf(existing -> existing.id().equals(job.id()));
            jobs.add(job);
            return job;
        }

        @Override
        public List<KnowledgeIndexJob> findRecentByProjectAndOwner(
                ProjectId projectId,
                UserId ownerUserId,
                int limit
        ) {
            return jobs.stream()
                    .filter(j -> j.projectId().equals(projectId) && j.ownerUserId().equals(ownerUserId))
                    .limit(limit)
                    .toList();
        }
    }
}
