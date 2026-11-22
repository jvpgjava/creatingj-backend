package io.github.jvpgjava.creatingj.conversation;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStatus;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStreamEvent;
import io.github.jvpgjava.creatingj.conversation.domain.MessageRole;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.ChatRole;
import io.github.jvpgjava.creatingj.inference.domain.GenerationChunk;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;
import io.github.jvpgjava.creatingj.inference.domain.ModelProviderException;
import io.github.jvpgjava.creatingj.inference.domain.PromptMessage;
import io.github.jvpgjava.creatingj.inference.domain.ProviderFailureKind;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationAndInferenceDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-22T13:30:00Z");

    @Test
    void shouldValidateConversationIdAndConversationInvariants() {
        ConversationId id = ConversationId.newId();
        assertThat(ConversationId.fromString(id.value().toString())).isEqualTo(id);
        assertThatThrownBy(() -> ConversationId.fromString("invalid")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ConversationId.fromString(" ")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new ConversationId(null)).isInstanceOf(DomainException.class);

        UserId owner = UserId.newId();
        Conversation conv = Conversation.create(id, owner, "  Chat Inicial  ", "z-ai/glm-5.3-flash", NOW);
        assertThat(conv.title()).isEqualTo("Chat Inicial");
        assertThat(conv.isOwnedBy(owner)).isTrue();
        assertThat(conv.isOwnedBy(UserId.newId())).isFalse();
        assertThat(conv.touch(NOW.plusSeconds(10)).updatedAt()).isEqualTo(NOW.plusSeconds(10));

        assertThatThrownBy(() -> Conversation.create(id, owner, " ", "m", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(id, owner, "a".repeat(161), "m", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(id, owner, "Ok", " ", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(id, owner, "Ok", "m".repeat(121), NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(null, owner, "Ok", "m", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(id, null, "Ok", "m", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(id, owner, null, "m", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Conversation.create(id, owner, "Ok", null, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldValidateChatMessageAndGenerationLifecycle() {
        ConversationId convId = ConversationId.newId();
        UserId owner = UserId.newId();

        ChatMessage userMsg = ChatMessage.create(UUID.randomUUID(), convId, MessageRole.USER, "Ola", 1, NOW);
        assertThat(userMsg.sequenceNum()).isEqualTo(1);
        assertThatThrownBy(() -> ChatMessage.create(UUID.randomUUID(), convId, MessageRole.USER, " ", 1, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ChatMessage.create(UUID.randomUUID(), convId, MessageRole.USER, "a".repeat(16001),
                1, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ChatMessage.create(UUID.randomUUID(), convId, MessageRole.USER, "Ola", 0, NOW))
                .isInstanceOf(DomainException.class);

        Generation started = Generation.streaming(
                UUID.randomUUID(), convId, owner, userMsg.id(), "z-ai/glm-5.3-flash", NOW);
        assertThat(started.status()).isEqualTo(GenerationStatus.STREAMING);
        assertThat(started.isTerminal()).isFalse();

        UUID assistantMsgId = UUID.randomUUID();
        Generation completed = started.complete(assistantMsgId, new TokenUsage(10, 20, 30), NOW.plusSeconds(5));
        assertThat(completed.status()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(completed.totalTokens()).isEqualTo(30);
        assertThat(completed.isTerminal()).isTrue();

        Generation cancelled = started.cancel(assistantMsgId, NOW.plusSeconds(2));
        assertThat(cancelled.status()).isEqualTo(GenerationStatus.CANCELLED);
        assertThat(cancelled.isTerminal()).isTrue();

        Generation failed = started.fail(ErrorCode.PROVIDER_TIMEOUT.name(), NOW.plusSeconds(3));
        assertThat(failed.status()).isEqualTo(GenerationStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(started.complete(assistantMsgId, null, NOW.plusSeconds(4)).totalTokens()).isEqualTo(0);

        assertThatThrownBy(() -> ChatMessage.create(null, convId, MessageRole.USER, "Ola", 1, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ChatMessage.create(UUID.randomUUID(), convId, null, "Ola", 1, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ChatMessage.create(UUID.randomUUID(), convId, MessageRole.USER, null, 1, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Generation.streaming(null, convId, owner, userMsg.id(), "m", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Generation.streaming(UUID.randomUUID(), convId, owner, userMsg.id(), " ", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Generation.streaming(UUID.randomUUID(), convId, owner, null, "m", NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldValidateInferenceDomainObjectsAndStreamEvents() {
        PromptMessage msg = new PromptMessage(ChatRole.USER, "Explique hexagonal");
        GenerationRequest req = new GenerationRequest("z-ai/glm-5.3-flash", List.of(msg), 2048, 0.2);
        assertThat(req.messages()).hasSize(1);

        assertThatThrownBy(() -> new PromptMessage(null, "hi")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new PromptMessage(ChatRole.USER, " ")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new GenerationRequest(" ", List.of(msg), 100, 0.2))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new GenerationRequest("m", List.of(), 100, 0.2))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new GenerationRequest("m", List.of(msg), 0, 0.2))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new GenerationRequest("m", List.of(msg), 5000, 0.2))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new GenerationRequest("m", List.of(msg), 100, 2.5))
                .isInstanceOf(DomainException.class);

        TokenUsage usage = new TokenUsage(5, 10, 15);
        GenerationChunk chunk = new GenerationChunk("Ola", null, usage);
        ModelCompletionResult result = new ModelCompletionResult("Ola", "m", "stop", usage);
        assertThat(chunk.deltaText()).isEqualTo("Ola");
        assertThat(result.content()).isEqualTo("Ola");

        var ex = new ModelProviderException(ProviderFailureKind.TIMEOUT, "Tempo limite excedido.");
        assertThat(ex.failureKind()).isEqualTo(ProviderFailureKind.TIMEOUT);
        assertThat(ex.toErrorCode()).isEqualTo(ErrorCode.PROVIDER_TIMEOUT);
        assertThat(new ModelProviderException(ProviderFailureKind.RATE_LIMITED, "rl").toErrorCode())
                .isEqualTo(ErrorCode.PROVIDER_RATE_LIMITED);
        assertThat(new ModelProviderException(ProviderFailureKind.UNAUTHORIZED, "unauth").toErrorCode())
                .isEqualTo(ErrorCode.BAD_GATEWAY);
        assertThat(new ModelProviderException(ProviderFailureKind.UNAVAILABLE, "down").toErrorCode())
                .isEqualTo(ErrorCode.PROVIDER_UNAVAILABLE);
        assertThat(new ModelProviderException(ProviderFailureKind.BAD_RESPONSE, "bad").toErrorCode())
                .isEqualTo(ErrorCode.BAD_GATEWAY);
        assertThat(new ModelProviderException(ProviderFailureKind.CANCELLED, "cancel").toErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE);

        UUID genId = UUID.randomUUID();
        var ev1 = GenerationStreamEvent.started(genId, 1);
        var ev2 = GenerationStreamEvent.delta(genId, 2, "abc");
        var ev3 = GenerationStreamEvent.completed(genId, 3, UUID.randomUUID(), usage);
        var ev3Empty = GenerationStreamEvent.completed(genId, 3, UUID.randomUUID(), null);
        var ev4 = GenerationStreamEvent.cancelled(genId, 4);
        var ev5 = GenerationStreamEvent.failed(genId, 5, ErrorCode.PROVIDER_UNAVAILABLE.name());
        assertThat(ev1.eventId()).isEqualTo(genId + ":1");
        assertThat(ev2.delta()).isEqualTo("abc");
        assertThat(ev3.status()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(ev3Empty.totalTokens()).isEqualTo(0);
        assertThat(ev4.status()).isEqualTo(GenerationStatus.CANCELLED);
        assertThat(ev5.errorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }
}
