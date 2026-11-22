package io.github.jvpgjava.creatingj.conversation.application.port.out;

import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;

import java.util.List;

public interface MessageStorePort {

    ChatMessage save(ChatMessage message);

    List<ChatMessage> findByConversationOrderBySequenceAsc(ConversationId conversationId);

    int nextSequenceNumber(ConversationId conversationId);
}
