package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import java.util.List;

public record ConversationHistoryResponse(
        ConversationSummaryResponse conversation,
        List<MessageResponse> messages,
        List<GenerationSummaryResponse> generations
) {
}
