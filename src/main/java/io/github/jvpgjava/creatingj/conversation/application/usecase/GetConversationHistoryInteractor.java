package io.github.jvpgjava.creatingj.conversation.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.application.port.in.GetConversationHistoryUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.MessageStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;

public final class GetConversationHistoryInteractor implements GetConversationHistoryUseCase {

    private final ConversationStorePort conversationStore;
    private final MessageStorePort messageStore;
    private final GenerationStorePort generationStore;

    public GetConversationHistoryInteractor(
            ConversationStorePort conversationStore,
            MessageStorePort messageStore,
            GenerationStorePort generationStore
    ) {
        this.conversationStore = conversationStore;
        this.messageStore = messageStore;
        this.generationStore = generationStore;
    }

    @Override
    public ConversationHistory execute(UserId actorUserId, ConversationId conversationId) {
        Conversation conversation = conversationStore.findByIdAndOwner(conversationId, actorUserId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Conversa nao encontrada."));

        List<ChatMessage> messages = messageStore.findByConversationOrderBySequenceAsc(conversationId);
        List<Generation> generations = generationStore.findByConversationOrderByCreatedAtDesc(conversationId);
        return new ConversationHistory(conversation, messages, generations);
    }
}
