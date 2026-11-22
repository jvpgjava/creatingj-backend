package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

public interface CreateConversationUseCase {

    Conversation execute(CreateConversationCommand command);

    record CreateConversationCommand(
            UserId actorUserId,
            String title,
            String modelId
    ) {
    }
}
