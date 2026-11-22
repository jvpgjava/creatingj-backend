package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;

public interface ListConversationsUseCase {

    ConversationsPage execute(ListConversationsQuery query);

    record ListConversationsQuery(UserId actorUserId, int page, int size) {
    }

    record ConversationsPage(List<Conversation> items, int page, int size, long totalElements) {
    }
}
