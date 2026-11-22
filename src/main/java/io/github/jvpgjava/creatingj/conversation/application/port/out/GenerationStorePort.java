package io.github.jvpgjava.creatingj.conversation.application.port.out;

import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationStorePort {

    Generation save(Generation generation);

    Optional<Generation> findByIdAndConversationAndOwner(
            UUID generationId,
            ConversationId conversationId,
            UserId ownerUserId
    );

    List<Generation> findByConversationOrderByCreatedAtDesc(ConversationId conversationId);
}
