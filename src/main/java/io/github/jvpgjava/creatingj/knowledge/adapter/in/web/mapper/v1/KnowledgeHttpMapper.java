package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.IngestKnowledgeSourceRequest;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.IngestKnowledgeSourceResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeIndexJobResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeIndexJobsResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchMatchResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchRequest;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSourceResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSourcesResponse;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.IngestKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.SearchProjectKnowledgeUseCase;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;

import java.util.List;

public final class KnowledgeHttpMapper {

    public IngestKnowledgeSourceUseCase.Command toIngestCommand(
            UserId actorUserId,
            String projectId,
            IngestKnowledgeSourceRequest request
    ) {
        return new IngestKnowledgeSourceUseCase.Command(
                actorUserId,
                ProjectId.parse(projectId),
                request.sourcePath(),
                request.title(),
                request.content()
        );
    }

    public SearchProjectKnowledgeUseCase.Query toSearchQuery(
            UserId actorUserId,
            String projectId,
            KnowledgeSearchRequest request
    ) {
        int topK = request.topK() == null ? 5 : request.topK();
        double minScore = request.minScore() == null ? 0.0 : request.minScore();
        return new SearchProjectKnowledgeUseCase.Query(
                actorUserId,
                ProjectId.parse(projectId),
                request.query(),
                topK,
                minScore
        );
    }

    public KnowledgeSourceResponse toSourceResponse(KnowledgeSource source) {
        return new KnowledgeSourceResponse(
                source.id().toString(),
                source.projectId().toString(),
                source.sourcePath(),
                source.title(),
                source.contentHash(),
                source.embeddingVersion(),
                source.status().name(),
                source.chunkCount(),
                source.createdAt(),
                source.updatedAt()
        );
    }

    public KnowledgeIndexJobResponse toJobResponse(KnowledgeIndexJob job) {
        return new KnowledgeIndexJobResponse(
                job.id().toString(),
                job.projectId().toString(),
                job.sourceId() == null ? null : job.sourceId().toString(),
                job.sourcePath(),
                job.operation().name(),
                job.status().name(),
                job.chunksIndexed(),
                job.errorCode(),
                job.createdAt(),
                job.completedAt()
        );
    }

    public IngestKnowledgeSourceResponse toIngestResponse(IngestKnowledgeSourceUseCase.Result result) {
        return new IngestKnowledgeSourceResponse(
                toSourceResponse(result.source()),
                toJobResponse(result.job()),
                result.deduplicated()
        );
    }

    public KnowledgeSourcesResponse toSourcesResponse(List<KnowledgeSource> sources) {
        return new KnowledgeSourcesResponse(sources.stream().map(this::toSourceResponse).toList());
    }

    public KnowledgeIndexJobsResponse toJobsResponse(List<KnowledgeIndexJob> jobs) {
        return new KnowledgeIndexJobsResponse(jobs.stream().map(this::toJobResponse).toList());
    }

    public KnowledgeSearchResponse toSearchResponse(SearchProjectKnowledgeUseCase.Result result) {
        List<KnowledgeSearchMatchResponse> matches = result.matches().stream()
                .map(this::toMatchResponse)
                .toList();
        return new KnowledgeSearchResponse(
                result.projectId().toString(),
                result.embeddingVersion(),
                result.latencyMs(),
                matches
        );
    }

    private KnowledgeSearchMatchResponse toMatchResponse(KnowledgeSearchMatch match) {
        return new KnowledgeSearchMatchResponse(
                match.sourceId().toString(),
                match.sourcePath(),
                match.title(),
                match.chunkIndex(),
                match.excerpt(),
                match.score(),
                match.embeddingVersion()
        );
    }
}
