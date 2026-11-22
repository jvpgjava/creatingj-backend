package io.github.jvpgjava.creatingj.conversation.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CreateConversationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;

import java.time.Instant;
import java.util.UUID;

public final class CreateConversationInteractor implements CreateConversationUseCase {

    private final ConversationStorePort conversationStore;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final String defaultModelId;

    public CreateConversationInteractor(
            ConversationStorePort conversationStore,
            AuditPort auditPort,
            ClockPort clock,
            String defaultModelId
    ) {
        this.conversationStore = conversationStore;
        this.auditPort = auditPort;
        this.clock = clock;
        this.defaultModelId = defaultModelId;
    }

    @Override
    public Conversation execute(CreateConversationCommand command) {
        Instant now = clock.now();
        String modelId = (command.modelId() == null || command.modelId().isBlank())
                ? defaultModelId
                : command.modelId().trim();

        Conversation conversation = Conversation.create(
                ConversationId.newId(),
                command.actorUserId(),
                command.title(),
                modelId,
                now
        );
        Conversation saved = conversationStore.save(conversation);

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.CONVERSATION_CREATED,
                "SUCCESS",
                "Conversa criada.",
                now
        ));

        return saved;
    }
}
