package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStreamEvent;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.function.Consumer;

public interface StreamMessageGenerationUseCase {

    void execute(StreamMessageCommand command, Consumer<GenerationStreamEvent> sink);

    record StreamMessageCommand(
            UserId actorUserId,
            ConversationId conversationId,
            String content
    ) {
    }
}
