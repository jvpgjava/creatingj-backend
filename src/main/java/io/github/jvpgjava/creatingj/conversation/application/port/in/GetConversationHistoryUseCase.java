package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;

public interface GetConversationHistoryUseCase {

    ConversationHistory execute(UserId actorUserId, ConversationId conversationId);

    record ConversationHistory(
            Conversation conversation,
            List<ChatMessage> messages,
            List<Generation> generations
    ) {
    }
}
