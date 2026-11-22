package io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1;

import java.util.List;

public record ProjectsPageResponse(
        List<ProjectResponse> items,
        int page,
        int size,
        long totalElements
) {
}
