package io.github.jvpgjava.creatingj.conversation;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CancelGenerationUseCase.CancelGenerationCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CreateConversationUseCase.CreateConversationCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.in.ListConversationsUseCase.ListConversationsQuery;
import io.github.jvpgjava.creatingj.conversation.application.port.in.SendMessageUseCase.SendMessageCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.in.StreamMessageGenerationUseCase.StreamMessageCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ActiveGenerationRegistryPort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.MessageStorePort;
import io.github.jvpgjava.creatingj.conversation.application.usecase.CancelGenerationInteractor;
import io.github.jvpgjava.creatingj.conversation.application.usecase.CreateConversationInteractor;
import io.github.jvpgjava.creatingj.conversation.application.usecase.GetConversationHistoryInteractor;
import io.github.jvpgjava.creatingj.conversation.application.usecase.ListConversationsInteractor;
import io.github.jvpgjava.creatingj.conversation.application.usecase.SendMessageInteractor;
import io.github.jvpgjava.creatingj.conversation.application.usecase.StreamMessageGenerationInteractor;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStatus;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStreamEvent;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.ModelGatewayPort;
import io.github.jvpgjava.creatingj.inference.domain.GenerationChunk;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;
import io.github.jvpgjava.creatingj.inference.domain.ModelProviderException;
import io.github.jvpgjava.creatingj.inference.domain.ProviderFailureKind;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-22T13:30:00Z");

    private ClockPort clock;
    private InMemoryConversationStore conversationStore;
    private InMemoryMessageStore messageStore;
    private InMemoryGenerationStore generationStore;
    private InMemoryActiveRegistry activeRegistry;
    private FakeModelGateway modelGateway;
    private List<AuditEvent> auditEvents;
    private AuditPort auditPort;

    private UserId alice;
    private UserId bob;

    private CreateConversationInteractor createConversation;
    private ListConversationsInteractor listConversations;
    private GetConversationHistoryInteractor getHistory;
    private SendMessageInteractor sendMessage;
    private StreamMessageGenerationInteractor streamMessage;
    private CancelGenerationInteractor cancelGeneration;

    @BeforeEach
    void setUp() {
        clock = () -> NOW;
        conversationStore = new InMemoryConversationStore();
        messageStore = new InMemoryMessageStore();
        generationStore = new InMemoryGenerationStore();
        activeRegistry = new InMemoryActiveRegistry();
        modelGateway = new FakeModelGateway();
        auditEvents = new ArrayList<>();
        auditPort = auditEvents::add;

        alice = UserId.newId();
        bob = UserId.newId();

        createConversation = new CreateConversationInteractor(
                conversationStore, auditPort, clock, "z-ai/glm-5.3-flash");
        listConversations = new ListConversationsInteractor(conversationStore);
        getHistory = new GetConversationHistoryInteractor(conversationStore, messageStore, generationStore);
        EnforceQuotaAndRecordUsageUseCase noOpQuota = new EnforceQuotaAndRecordUsageUseCase() {
            @Override
            public void acquirePermit(UserId userId) {
            }

            @Override
            public UsageRecord recordAndRelease(RecordUsageCommand command) {
                return new UsageRecord(UUID.randomUUID(), command.userId(), command.conversationId(),
                        command.generationId(), command.modelId(), command.source(), 0, 0, 0, true, 0,
                        command.outcome(), NOW);
            }
        };

        sendMessage = new SendMessageInteractor(
                conversationStore, messageStore, generationStore, modelGateway, noOpQuota, auditPort, clock, 2048);
        streamMessage = new StreamMessageGenerationInteractor(
                conversationStore, messageStore, generationStore, activeRegistry, modelGateway,
                noOpQuota, auditPort, clock, 2048);
        cancelGeneration = new CancelGenerationInteractor(
                conversationStore, generationStore, activeRegistry, auditPort, clock);
    }

    @Test
    void shouldCreateListSendMessageAndIsolateConversationsCrossUser() {
        Conversation conv = createConversation.execute(new CreateConversationCommand(alice, "Meu Chat", null));
        assertThat(conv.modelId()).isEqualTo("z-ai/glm-5.3-flash");

        var sent = sendMessage.execute(new SendMessageCommand(alice, conv.id(), "Ola IA"));
        assertThat(sent.userMessage().content()).isEqualTo("Ola IA");
        assertThat(sent.assistantMessage().content()).isEqualTo("Resposta simulada");
        assertThat(sent.generation().status()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(sent.generation().totalTokens()).isEqualTo(25);

        var history = getHistory.execute(alice, conv.id());
        assertThat(history.messages()).hasSize(2);
        assertThat(history.generations()).hasSize(1);

        var aliceList = listConversations.execute(new ListConversationsQuery(alice, 0, 10));
        var bobList = listConversations.execute(new ListConversationsQuery(bob, 0, 10));
        assertThat(aliceList.items()).hasSize(1);
        assertThat(bobList.items()).isEmpty();

        assertThatThrownBy(() -> getHistory.execute(bob, conv.id()))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);

        assertThatThrownBy(() -> sendMessage.execute(new SendMessageCommand(bob, conv.id(), "Intrusao")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);

        assertThatThrownBy(() -> listConversations.execute(new ListConversationsQuery(alice, -1, 10)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldStreamGenerationEmitOrderedEventsAndSupportCancellation() {
        Conversation conv = createConversation.execute(new CreateConversationCommand(alice, "Stream Chat", null));
        List<GenerationStreamEvent> events = new ArrayList<>();

        streamMessage.execute(new StreamMessageCommand(alice, conv.id(), "Conte uma historia"), events::add);

        assertThat(events).hasSize(4);
        assertThat(events.get(0).type()).isEqualTo("generation.started");
        assertThat(events.get(1).delta()).isEqualTo("Parte 1 ");
        assertThat(events.get(2).delta()).isEqualTo("Parte 2");
        assertThat(events.get(3).type()).isEqualTo("generation.completed");

        var history = getHistory.execute(alice, conv.id());
        assertThat(history.messages()).hasSize(2);
        assertThat(history.messages().get(1).content()).isEqualTo("Parte 1 Parte 2");

        modelGateway.cancelOnFirstChunk = true;
        List<GenerationStreamEvent> cancelEvents = new ArrayList<>();
        streamMessage.execute(new StreamMessageCommand(alice, conv.id(), "Cancelar"), event -> {
            cancelEvents.add(event);
            if ("generation.started".equals(event.type())) {
                cancelGeneration.execute(new CancelGenerationCommand(alice, conv.id(), event.generationId()));
            }
        });

        assertThat(cancelEvents).anyMatch(e -> "generation.cancelled".equals(e.type()));
        assertThat(auditEvents).anyMatch(e -> e.action() == AuditAction.GENERATION_CANCELLED);
    }

    @Test
    void shouldTranslateProviderFailuresWithoutLeakingAndPersistFailedGeneration() {
        Conversation conv = createConversation.execute(new CreateConversationCommand(alice, "Erro Chat", null));
        modelGateway.nextError = new ModelProviderException(
                ProviderFailureKind.RATE_LIMITED, "Provedor temporariamente limitado.");

        assertThatThrownBy(() -> sendMessage.execute(new SendMessageCommand(alice, conv.id(), "Oi")))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROVIDER_RATE_LIMITED);

        var history = getHistory.execute(alice, conv.id());
        assertThat(history.generations()).hasSize(1);
        assertThat(history.generations().get(0).status()).isEqualTo(GenerationStatus.FAILED);
        assertThat(auditEvents).anyMatch(e -> e.action() == AuditAction.PROVIDER_ERROR);

        List<GenerationStreamEvent> streamEvents = new ArrayList<>();
        streamMessage.execute(new StreamMessageCommand(alice, conv.id(), "Oi stream"), streamEvents::add);
        assertThat(streamEvents).anyMatch(e -> "generation.failed".equals(e.type())
                && "PROVIDER_RATE_LIMITED".equals(e.errorCode()));
    }

    @Test
    void shouldPreventCrossUserCancellationAndHandleEdgeCases() {
        Conversation conv = createConversation.execute(
                new CreateConversationCommand(alice, "Privado", "qwen/qwen3.5-9b"));
        assertThat(conv.modelId()).isEqualTo("qwen/qwen3.5-9b");

        var sent = sendMessage.execute(new SendMessageCommand(alice, conv.id(), "Oi"));
        var idempotentCancel = cancelGeneration.execute(
                new CancelGenerationCommand(alice, conv.id(), sent.generation().id()));
        assertThat(idempotentCancel.status()).isEqualTo(GenerationStatus.COMPLETED);

        assertThatThrownBy(() -> cancelGeneration.execute(
                new CancelGenerationCommand(bob, conv.id(), sent.generation().id())))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);

        assertThatThrownBy(() -> listConversations.execute(new ListConversationsQuery(alice, 0, 0)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> listConversations.execute(new ListConversationsQuery(alice, 0, 101)))
                .isInstanceOf(DomainException.class);

        Conversation longConv = createConversation.execute(new CreateConversationCommand(alice, "Janela", " "));
        for (int i = 0; i < 12; i++) {
            sendMessage.execute(new SendMessageCommand(alice, longConv.id(), "Msg " + i + " " + "x".repeat(3000)));
        }
        assertThat(getHistory.execute(alice, longConv.id()).messages()).hasSize(24);

        modelGateway.cancelOnFirstChunk = true;
        List<GenerationStreamEvent> partialCancelEvents = new ArrayList<>();
        streamMessage.execute(new StreamMessageCommand(alice, conv.id(), "Parcial"), event -> {
            partialCancelEvents.add(event);
            if ("generation.delta".equals(event.type())) {
                cancelGeneration.execute(new CancelGenerationCommand(alice, conv.id(), event.generationId()));
            }
        });
        assertThat(partialCancelEvents).anyMatch(e -> "generation.cancelled".equals(e.type()));
    }

    private static final class InMemoryConversationStore implements ConversationStorePort {
        private final Map<ConversationId, Conversation> map = new HashMap<>();

        @Override
        public Conversation save(Conversation conversation) {
            map.put(conversation.id(), conversation);
            return conversation;
        }

        @Override
        public Optional<Conversation> findByIdAndOwner(ConversationId id, UserId ownerUserId) {
            return Optional.ofNullable(map.get(id)).filter(c -> c.isOwnedBy(ownerUserId));
        }

        @Override
        public List<Conversation> findPageByOwner(UserId ownerUserId, int page, int size) {
            return map.values().stream()
                    .filter(c -> c.isOwnedBy(ownerUserId))
                    .sorted(Comparator.comparing(Conversation::updatedAt).reversed())
                    .skip((long) page * size)
                    .limit(size)
                    .toList();
        }

        @Override
        public long countByOwner(UserId ownerUserId) {
            return map.values().stream().filter(c -> c.isOwnedBy(ownerUserId)).count();
        }
    }

    private static final class InMemoryMessageStore implements MessageStorePort {
        private final List<ChatMessage> list = new ArrayList<>();

        @Override
        public ChatMessage save(ChatMessage message) {
            list.add(message);
            return message;
        }

        @Override
        public List<ChatMessage> findByConversationOrderBySequenceAsc(ConversationId conversationId) {
            return list.stream()
                    .filter(m -> m.conversationId().equals(conversationId))
                    .sorted(Comparator.comparingInt(ChatMessage::sequenceNum))
                    .toList();
        }

        @Override
        public int nextSequenceNumber(ConversationId conversationId) {
            return list.stream()
                    .filter(m -> m.conversationId().equals(conversationId))
                    .mapToInt(ChatMessage::sequenceNum)
                    .max()
                    .orElse(0) + 1;
        }
    }

    private static final class InMemoryGenerationStore implements GenerationStorePort {
        private final Map<UUID, Generation> map = new HashMap<>();

        @Override
        public Generation save(Generation generation) {
            map.put(generation.id(), generation);
            return generation;
        }

        @Override
        public Optional<Generation> findByIdAndConversationAndOwner(
                UUID generationId,
                ConversationId conversationId,
                UserId ownerUserId
        ) {
            return Optional.ofNullable(map.get(generationId))
                    .filter(g -> g.conversationId().equals(conversationId) && g.ownerUserId().equals(ownerUserId));
        }

        @Override
        public List<Generation> findByConversationOrderByCreatedAtDesc(ConversationId conversationId) {
            return map.values().stream()
                    .filter(g -> g.conversationId().equals(conversationId))
                    .sorted(Comparator.comparing(Generation::createdAt).reversed())
                    .toList();
        }
    }

    private static final class InMemoryActiveRegistry implements ActiveGenerationRegistryPort {
        private final Map<UUID, AtomicBoolean> active = new HashMap<>();

        @Override
        public void register(UUID generationId) {
            active.put(generationId, new AtomicBoolean(false));
        }

        @Override
        public boolean requestCancel(UUID generationId) {
            AtomicBoolean flag = active.get(generationId);
            if (flag == null) {
                return false;
            }
            flag.set(true);
            return true;
        }

        @Override
        public boolean isCancelled(UUID generationId) {
            AtomicBoolean flag = active.get(generationId);
            return flag != null && flag.get();
        }

        @Override
        public void unregister(UUID generationId) {
            active.remove(generationId);
        }
    }

    private static final class FakeModelGateway implements ModelGatewayPort {
        private ModelProviderException nextError;
        private boolean cancelOnFirstChunk;

        @Override
        public ModelCompletionResult complete(GenerationRequest request) {
            if (nextError != null) {
                throw nextError;
            }
            return new ModelCompletionResult("Resposta simulada", request.modelId(), "stop",
                    new TokenUsage(10, 15, 25));
        }

        @Override
        public ModelCompletionResult stream(
                GenerationRequest request,
                Consumer<GenerationChunk> onChunk,
                BooleanSupplier cancelled
        ) {
            if (nextError != null) {
                throw nextError;
            }
            if (cancelled.getAsBoolean()) {
                throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Geracao cancelada.");
            }
            onChunk.accept(new GenerationChunk("Parte 1 ", null, null));
            if (cancelOnFirstChunk && cancelled.getAsBoolean()) {
                throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Geracao cancelada.");
            }
            TokenUsage usage = new TokenUsage(12, 18, 30);
            onChunk.accept(new GenerationChunk("Parte 2", "stop", usage));
            return new ModelCompletionResult("Parte 1 Parte 2", request.modelId(), "stop", usage);
        }

        @Override
        public List<String> listAvailableModels() {
            return List.of("z-ai/glm-5.3-flash");
        }
    }
}
