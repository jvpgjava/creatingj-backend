package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

public record MessageExchangeResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        GenerationSummaryResponse generation
) {
}
