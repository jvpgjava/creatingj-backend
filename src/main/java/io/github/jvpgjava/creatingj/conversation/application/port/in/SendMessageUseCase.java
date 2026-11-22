package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

public interface SendMessageUseCase {

    MessageExchangeResult execute(SendMessageCommand command);

    record SendMessageCommand(
            UserId actorUserId,
            ConversationId conversationId,
            String content
    ) {
    }

    record MessageExchangeResult(
            ChatMessage userMessage,
            ChatMessage assistantMessage,
            Generation generation
    ) {
    }
}
