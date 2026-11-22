package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.apidocs.v1.KnowledgeApiDocs;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.IngestKnowledgeSourceRequest;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.IngestKnowledgeSourceResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeIndexJobsResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchRequest;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSourcesResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.mapper.v1.KnowledgeHttpMapper;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.DeleteKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.IngestKnowledgeSourceUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeIndexJobsUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.ListKnowledgeSourcesUseCase;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.SearchProjectKnowledgeUseCase;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/knowledge")
public final class KnowledgeController implements KnowledgeApiDocs {

    private final IngestKnowledgeSourceUseCase ingestKnowledgeSource;
    private final ListKnowledgeSourcesUseCase listKnowledgeSources;
    private final DeleteKnowledgeSourceUseCase deleteKnowledgeSource;
    private final SearchProjectKnowledgeUseCase searchProjectKnowledge;
    private final ListKnowledgeIndexJobsUseCase listKnowledgeIndexJobs;
    private final KnowledgeHttpMapper mapper;

    public KnowledgeController(
            IngestKnowledgeSourceUseCase ingestKnowledgeSource,
            ListKnowledgeSourcesUseCase listKnowledgeSources,
            DeleteKnowledgeSourceUseCase deleteKnowledgeSource,
            SearchProjectKnowledgeUseCase searchProjectKnowledge,
            ListKnowledgeIndexJobsUseCase listKnowledgeIndexJobs,
            KnowledgeHttpMapper mapper
    ) {
        this.ingestKnowledgeSource = ingestKnowledgeSource;
        this.listKnowledgeSources = listKnowledgeSources;
        this.deleteKnowledgeSource = deleteKnowledgeSource;
        this.searchProjectKnowledge = searchProjectKnowledge;
        this.listKnowledgeIndexJobs = listKnowledgeIndexJobs;
        this.mapper = mapper;
    }

    @Override
    @PostMapping("/sources")
    public ResponseEntity<IngestKnowledgeSourceResponse> ingest(
            @PathVariable String projectId,
            @Valid @RequestBody IngestKnowledgeSourceRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = ingestKnowledgeSource.execute(mapper.toIngestCommand(actor.id(), projectId, request));
        return ResponseEntity.ok(mapper.toIngestResponse(result));
    }

    @Override
    @GetMapping("/sources")
    public ResponseEntity<KnowledgeSourcesResponse> listSources(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var sources = listKnowledgeSources.execute(
                new ListKnowledgeSourcesUseCase.Query(actor.id(), ProjectId.parse(projectId))
        );
        return ResponseEntity.ok(mapper.toSourcesResponse(sources));
    }

    @Override
    @DeleteMapping("/sources/{sourceId}")
    public ResponseEntity<Void> deleteSource(
            @PathVariable String projectId,
            @PathVariable String sourceId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        deleteKnowledgeSource.execute(new DeleteKnowledgeSourceUseCase.Command(
                actor.id(),
                ProjectId.parse(projectId),
                KnowledgeSourceId.parse(sourceId)
        ));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/search")
    public ResponseEntity<KnowledgeSearchResponse> search(
            @PathVariable String projectId,
            @Valid @RequestBody KnowledgeSearchRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = searchProjectKnowledge.execute(mapper.toSearchQuery(actor.id(), projectId, request));
        return ResponseEntity.ok(mapper.toSearchResponse(result));
    }

    @Override
    @GetMapping("/jobs")
    public ResponseEntity<KnowledgeIndexJobsResponse> listJobs(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var jobs = listKnowledgeIndexJobs.execute(
                new ListKnowledgeIndexJobsUseCase.Query(actor.id(), ProjectId.parse(projectId), limit)
        );
        return ResponseEntity.ok(mapper.toJobsResponse(jobs));
    }
}
