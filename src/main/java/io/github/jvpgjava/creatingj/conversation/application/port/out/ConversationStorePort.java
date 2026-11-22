package io.github.jvpgjava.creatingj.conversation.application.port.out;

import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;
import java.util.Optional;

public interface ConversationStorePort {

    Conversation save(Conversation conversation);

    Optional<Conversation> findByIdAndOwner(ConversationId id, UserId ownerUserId);

    List<Conversation> findPageByOwner(UserId ownerUserId, int page, int size);

    long countByOwner(UserId ownerUserId);
}
