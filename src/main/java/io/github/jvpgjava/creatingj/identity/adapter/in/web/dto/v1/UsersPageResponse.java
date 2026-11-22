package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import java.util.List;

public record UsersPageResponse(
        List<UserSummaryResponse> items,
        int page,
        int size,
        long totalElements
) {
}
