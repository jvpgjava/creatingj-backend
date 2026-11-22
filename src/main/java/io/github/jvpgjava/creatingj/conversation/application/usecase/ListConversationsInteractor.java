package io.github.jvpgjava.creatingj.conversation.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.application.port.in.ListConversationsUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;

import java.util.List;

public final class ListConversationsInteractor implements ListConversationsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationStorePort conversationStore;

    public ListConversationsInteractor(ConversationStorePort conversationStore) {
        this.conversationStore = conversationStore;
    }

    @Override
    public ConversationsPage execute(ListConversationsQuery query) {
        if (query.page() < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametro page deve ser maior ou igual a zero.");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametro size deve estar entre 1 e 100.");
        }
        List<Conversation> items = conversationStore.findPageByOwner(query.actorUserId(), query.page(), query.size());
        long total = conversationStore.countByOwner(query.actorUserId());
        return new ConversationsPage(items, query.page(), query.size(), total);
    }
}
