package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import java.util.List;

public record KnowledgeSourcesResponse(
        List<KnowledgeSourceResponse> items
) {
}
