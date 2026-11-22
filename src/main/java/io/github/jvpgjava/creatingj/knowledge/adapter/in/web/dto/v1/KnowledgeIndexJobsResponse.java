package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lista dos jobs de indexacao de conhecimento do projeto")
public record KnowledgeIndexJobsResponse(
        @Schema(description = "Jobs ordenados do mais recente ao mais antigo")
        List<KnowledgeIndexJobResponse> items
) {
}
