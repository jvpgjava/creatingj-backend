package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import java.util.List;

public record ConversationsPageResponse(
        List<ConversationSummaryResponse> items,
        int page,
        int size,
        long totalElements
) {
}
