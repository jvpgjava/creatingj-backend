package io.github.jvpgjava.creatingj.conversation.application.usecase;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.application.port.in.SendMessageUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.MessageStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.conversation.domain.MessageRole;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase.RecordUsageCommand;
import io.github.jvpgjava.creatingj.inference.application.port.out.ModelGatewayPort;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;
import io.github.jvpgjava.creatingj.inference.domain.ModelProviderException;
import io.github.jvpgjava.creatingj.inference.domain.PromptMessage;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SendMessageInteractor implements SendMessageUseCase {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    private final ConversationStorePort conversationStore;
    private final MessageStorePort messageStore;
    private final GenerationStorePort generationStore;
    private final ModelGatewayPort modelGateway;
    private final EnforceQuotaAndRecordUsageUseCase quotaUsage;
    private final AuditPort auditPort;
    private final ClockPort clock;
    private final int defaultMaxTokens;

    public SendMessageInteractor(
            ConversationStorePort conversationStore,
            MessageStorePort messageStore,
            GenerationStorePort generationStore,
            ModelGatewayPort modelGateway,
            EnforceQuotaAndRecordUsageUseCase quotaUsage,
            AuditPort auditPort,
            ClockPort clock,
            int defaultMaxTokens
    ) {
        this.conversationStore = conversationStore;
        this.messageStore = messageStore;
        this.generationStore = generationStore;
        this.modelGateway = modelGateway;
        this.quotaUsage = quotaUsage;
        this.auditPort = auditPort;
        this.clock = clock;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    @Override
    public MessageExchangeResult execute(SendMessageCommand command) {
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

        Generation generation = generationStore.save(Generation.streaming(
                UUID.randomUUID(),
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

        TokenUsage reportedUsage = TokenUsage.empty();
        int completionChars = 0;
        UsageOutcome outcome = UsageOutcome.FAILED;
        try {
            ModelCompletionResult completion = modelGateway.complete(request);
            Instant finishedAt = clock.now();
            String assistantContent = (completion.content() == null || completion.content().isBlank())
                    ? "(sem resposta textual)"
                    : completion.content();

            reportedUsage = completion.usage();
            completionChars = assistantContent.length();
            outcome = UsageOutcome.COMPLETED;

            int assistantSeq = messageStore.nextSequenceNumber(conversation.id());
            ChatMessage assistantMessage = messageStore.save(ChatMessage.create(
                    UUID.randomUUID(),
                    conversation.id(),
                    MessageRole.ASSISTANT,
                    assistantContent,
                    assistantSeq,
                    finishedAt
            ));

            Generation completed = generationStore.save(
                    generation.complete(assistantMessage.id(), completion.usage(), finishedAt));
            conversationStore.save(conversation.touch(finishedAt));
            return new MessageExchangeResult(userMessage, assistantMessage, completed);
        } catch (ModelProviderException ex) {
            Instant failedAt = clock.now();
            ErrorCode code = ex.toErrorCode();
            generationStore.save(generation.fail(code.name(), failedAt));
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    command.actorUserId().value(),
                    command.actorUserId().value(),
                    AuditAction.PROVIDER_ERROR,
                    "FAILURE",
                    "Falha na comunicacao com o provedor de IA: " + code.name(),
                    failedAt
            ));
            throw new DomainException(code, ex.getMessage(), ex);
        } finally {
            long latencyMs = Math.max(0L, Duration.between(startedAt, clock.now()).toMillis());
            quotaUsage.recordAndRelease(new RecordUsageCommand(
                    command.actorUserId(),
                    conversation.id().value(),
                    generation.id(),
                    conversation.modelId(),
                    UsageSource.CHAT_SYNC,
                    reportedUsage,
                    promptChars,
                    completionChars,
                    latencyMs,
                    outcome
            ));
        }
    }
}
