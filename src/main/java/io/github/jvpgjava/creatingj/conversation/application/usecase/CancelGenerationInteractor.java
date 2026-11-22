package io.github.jvpgjava.creatingj.conversation.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CancelGenerationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ActiveGenerationRegistryPort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;

import java.time.Instant;
import java.util.UUID;

public final class CancelGenerationInteractor implements CancelGenerationUseCase {

    private final ConversationStorePort conversationStore;
    private final GenerationStorePort generationStore;
    private final ActiveGenerationRegistryPort activeRegistry;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public CancelGenerationInteractor(
            ConversationStorePort conversationStore,
            GenerationStorePort generationStore,
            ActiveGenerationRegistryPort activeRegistry,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.conversationStore = conversationStore;
        this.generationStore = generationStore;
        this.activeRegistry = activeRegistry;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Generation execute(CancelGenerationCommand command) {
        conversationStore.findByIdAndOwner(command.conversationId(), command.actorUserId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Conversa nao encontrada."));

        Generation generation = generationStore.findByIdAndConversationAndOwner(
                        command.generationId(), command.conversationId(), command.actorUserId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Geracao nao encontrada."));

        if (generation.isTerminal()) {
            return generation;
        }

        Instant now = clock.now();
        activeRegistry.requestCancel(generation.id());
        Generation cancelled = generationStore.save(generation.cancel(generation.assistantMessageId(), now));

        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.GENERATION_CANCELLED,
                "SUCCESS",
                "Geracao cancelada pelo proprietario.",
                now
        ));

        return cancelled;
    }
}
