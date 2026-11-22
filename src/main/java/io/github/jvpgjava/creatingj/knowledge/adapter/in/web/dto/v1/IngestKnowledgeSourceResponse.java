package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

public record IngestKnowledgeSourceResponse(
        KnowledgeSourceResponse source,
        KnowledgeIndexJobResponse job,
        boolean deduplicated
) {
}
