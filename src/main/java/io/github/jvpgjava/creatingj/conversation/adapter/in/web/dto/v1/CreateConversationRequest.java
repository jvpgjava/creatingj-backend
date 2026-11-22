package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 120) String modelId
) {
}
