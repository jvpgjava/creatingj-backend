package io.github.jvpgjava.creatingj.conversation.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.application.port.in.StreamMessageGenerationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ActiveGenerationRegistryPort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.MessageStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStreamEvent;
import io.github.jvpgjava.creatingj.conversation.domain.MessageRole;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase.RecordUsageCommand;
import io.github.jvpgjava.creatingj.inference.application.port.out.ModelGatewayPort;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;
import io.github.jvpgjava.creatingj.inference.domain.ModelProviderException;
import io.github.jvpgjava.creatingj.inference.domain.PromptMessage;
import io.github.jvpgjava.creatingj.inference.domain.ProviderFailureKind;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class StreamMessageGenerationInteractor implements StreamMessageGenerationUseCase {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    private final ConversationStorePort conversationStore;
    private final MessageStorePort messageStore;
    private final GenerationStorePort generationStore;
    private final ActiveGenerationRegistryPort activeRegistry;
    private final ModelGatewayPort modelGateway;
    private final EnforceQuotaAndRecordUsageUseCase quotaUsage;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final int defaultMaxTokens;

    public StreamMessageGenerationInteractor(
            ConversationStorePort conversationStore,
            MessageStorePort messageStore,
            GenerationStorePort generationStore,
            ActiveGenerationRegistryPort activeRegistry,
            ModelGatewayPort modelGateway,
            EnforceQuotaAndRecordUsageUseCase quotaUsage,
            AuditPort auditPort,
            ClockPort clock,
            int defaultMaxTokens
    ) {
        this.conversationStore = conversationStore;
        this.messageStore = messageStore;
        this.generationStore = generationStore;
        this.activeRegistry = activeRegistry;
        this.modelGateway = modelGateway;
        this.quotaUsage = quotaUsage;
        this.auditPort = auditPort;
        this.clock = clock;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    @Override
    public void execute(StreamMessageCommand command, Consumer<GenerationStreamEvent> sink) {
        Conversation conversation = conversationStore.findByIdAndOwner(command.conversationId(), command.actorUserId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Conversa nao encontrada."));

        quotaUsage.acquirePermit(command.actorUserId());
        Instant startedAt = clock.now();

        int userSeq = messageStore.nextSequenceNumber(conversation.id());
        ChatMessage userMessage = messageStore.save(ChatMessage.create(
                UUID.randomUUID(),
                conversation.id(),
                MessageRole.USER,
                command.content(),
                userSeq,
                startedAt
        ));

        UUID generationId = UUID.randomUUID();
        Generation generation = generationStore.save(Generation.streaming(
                generationId,
                conversation.id(),
                command.actorUserId(),
                userMessage.id(),
                conversation.modelId(),
                startedAt
        ));

        List<ChatMessage> history = messageStore.findByConversationOrderBySequenceAsc(conversation.id());
        List<PromptMessage> promptMessages = PromptWindowAssembler.assemble(history);
        int promptChars = promptMessages.stream().mapToInt(m -> m.content().length()).sum();
        GenerationRequest request = new GenerationRequest(
                conversation.modelId(),
                promptMessages,
                defaultMaxTokens,
                DEFAULT_TEMPERATURE
        );

        activeRegistry.register(generationId);
        AtomicInteger eventSeq = new AtomicInteger(1);
        StringBuilder partial = new StringBuilder();
        TokenUsage reportedUsage = TokenUsage.empty();
        UsageOutcome outcome = UsageOutcome.FAILED;
        try {
            sink.accept(GenerationStreamEvent.started(generationId, eventSeq.getAndIncrement()));

            ModelCompletionResult result = modelGateway.stream(
                    request,
                    chunk -> {
                        if (chunk.deltaText() != null && !chunk.deltaText().isEmpty()) {
                            partial.append(chunk.deltaText());
                            sink.accept(GenerationStreamEvent.delta(
                                    generationId, eventSeq.getAndIncrement(), chunk.deltaText()));
                        }
                    },
                    () -> activeRegistry.isCancelled(generationId)
            );

            Instant finishedAt = clock.now();
            String finalContent = result.content() == null || result.content().isBlank()
                    ? partial.toString()
                    : result.content();
            if (finalContent.isBlank()) {
                finalContent = "(sem resposta textual)";
            }

            reportedUsage = result.usage();
            outcome = UsageOutcome.COMPLETED;

            int assistantSeq = messageStore.nextSequenceNumber(conversation.id());
            ChatMessage assistantMessage = messageStore.save(ChatMessage.create(
                    UUID.randomUUID(),
                    conversation.id(),
                    MessageRole.ASSISTANT,
                    finalContent,
                    assistantSeq,
                    finishedAt
            ));

            generationStore.save(generation.complete(assistantMessage.id(), result.usage(), finishedAt));
            conversationStore.save(conversation.touch(finishedAt));
            sink.accept(GenerationStreamEvent.completed(
                    generationId, eventSeq.getAndIncrement(), assistantMessage.id(), result.usage()));
        } catch (ModelProviderException ex) {
            Instant endedAt = clock.now();
            if (ex.failureKind() == ProviderFailureKind.CANCELLED || activeRegistry.isCancelled(generationId)) {
                outcome = UsageOutcome.CANCELLED;
                UUID partialMsgId = persistPartialIfPresent(conversation, partial.toString(), endedAt);
                generationStore.save(generation.cancel(partialMsgId, endedAt));
                sink.accept(GenerationStreamEvent.cancelled(generationId, eventSeq.getAndIncrement()));
            } else {
                ErrorCode code = ex.toErrorCode();
                generationStore.save(generation.fail(code.name(), endedAt));
                auditPort.record(AuditEvent.of(
                        UUID.randomUUID(),
                        command.actorUserId().value(),
                        command.actorUserId().value(),
                        AuditAction.PROVIDER_ERROR,
                        "FAILURE",
                        "Falha de streaming com provedor de IA: " + code.name(),
                        endedAt
                ));
                sink.accept(GenerationStreamEvent.failed(generationId, eventSeq.getAndIncrement(), code.name()));
            }
        } finally {
            activeRegistry.unregister(generationId);
            long latencyMs = Math.max(0L, Duration.between(startedAt, clock.now()).toMillis());
            quotaUsage.recordAndRelease(new RecordUsageCommand(
                    command.actorUserId(),
                    conversation.id().value(),
                    generationId,
                    conversation.modelId(),
                    UsageSource.CHAT_STREAM,
                    reportedUsage,
                    promptChars,
                    partial.length(),
                    latencyMs,
                    outcome
            ));
        }
    }

    private UUID persistPartialIfPresent(Conversation conversation, String partialText, Instant now) {
        if (partialText == null || partialText.isBlank()) {
            return null;
        }
        int seq = messageStore.nextSequenceNumber(conversation.id());
        ChatMessage partialMessage = messageStore.save(ChatMessage.create(
                UUID.randomUUID(),
                conversation.id(),
                MessageRole.ASSISTANT,
                partialText,
                seq,
                now
        ));
        return partialMessage.id();
    }
}
