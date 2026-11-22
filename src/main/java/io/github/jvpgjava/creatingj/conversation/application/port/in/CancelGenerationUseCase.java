package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.UUID;

public interface CancelGenerationUseCase {

    Generation execute(CancelGenerationCommand command);

    record CancelGenerationCommand(
            UserId actorUserId,
            ConversationId conversationId,
            UUID generationId
    ) {
    }
}
