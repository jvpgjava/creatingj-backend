package io.github.jvpgjava.creatingj.conversation.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationHistoryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationSummaryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationsPageResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.CreateConversationRequest;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.GenerationSummaryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.MessageExchangeResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.MessageResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.SendMessageRequest;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.SseGenerationEventPayload;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CancelGenerationUseCase.CancelGenerationCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CreateConversationUseCase.CreateConversationCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.in.GetConversationHistoryUseCase.ConversationHistory;
import io.github.jvpgjava.creatingj.conversation.application.port.in.ListConversationsUseCase.ConversationsPage;
import io.github.jvpgjava.creatingj.conversation.application.port.in.ListConversationsUseCase.ListConversationsQuery;
import io.github.jvpgjava.creatingj.conversation.application.port.in.SendMessageUseCase.MessageExchangeResult;
import io.github.jvpgjava.creatingj.conversation.application.port.in.SendMessageUseCase.SendMessageCommand;
import io.github.jvpgjava.creatingj.conversation.application.port.in.StreamMessageGenerationUseCase.StreamMessageCommand;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStreamEvent;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class ConversationHttpMapper {

    public CreateConversationCommand toCreateCommand(UserId actorId, CreateConversationRequest request) {
        return new CreateConversationCommand(actorId, request.title(), request.modelId());
    }

    public ListConversationsQuery toListQuery(UserId actorId, int page, int size) {
        return new ListConversationsQuery(actorId, page, size);
    }

    public SendMessageCommand toSendMessageCommand(
            UserId actorId,
            String conversationIdRaw,
            SendMessageRequest request
    ) {
        return new SendMessageCommand(actorId, ConversationId.fromString(conversationIdRaw), request.content());
    }

    public StreamMessageCommand toStreamCommand(
            UserId actorId,
            String conversationIdRaw,
            SendMessageRequest request
    ) {
        return new StreamMessageCommand(actorId, ConversationId.fromString(conversationIdRaw), request.content());
    }

    public CancelGenerationCommand toCancelCommand(
            UserId actorId,
            String conversationIdRaw,
            String generationIdRaw
    ) {
        return new CancelGenerationCommand(
                actorId,
                ConversationId.fromString(conversationIdRaw),
                parseUuid(generationIdRaw)
        );
    }

    public ConversationSummaryResponse toSummary(Conversation conversation) {
        return new ConversationSummaryResponse(
                conversation.id().value().toString(),
                conversation.title(),
                conversation.modelId(),
                conversation.createdAt(),
                conversation.updatedAt()
        );
    }

    public ConversationsPageResponse toPage(ConversationsPage page) {
        return new ConversationsPageResponse(
                page.items().stream().map(this::toSummary).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }

    public ConversationHistoryResponse toHistory(ConversationHistory history) {
        return new ConversationHistoryResponse(
                toSummary(history.conversation()),
                history.messages().stream().map(this::toMessage).toList(),
                history.generations().stream().map(this::toGeneration).toList()
        );
    }

    public MessageExchangeResponse toExchange(MessageExchangeResult result) {
        return new MessageExchangeResponse(
                toMessage(result.userMessage()),
                toMessage(result.assistantMessage()),
                toGeneration(result.generation())
        );
    }

    public MessageResponse toMessage(ChatMessage message) {
        return new MessageResponse(
                message.id().toString(),
                message.role().name(),
                message.content(),
                message.sequenceNum(),
                message.createdAt()
        );
    }

    public GenerationSummaryResponse toGeneration(Generation generation) {
        return new GenerationSummaryResponse(
                generation.id().toString(),
                generation.modelId(),
                generation.status().name(),
                generation.promptTokens(),
                generation.completionTokens(),
                generation.totalTokens(),
                generation.errorCode(),
                generation.createdAt(),
                generation.completedAt()
        );
    }

    public SseGenerationEventPayload toSsePayload(GenerationStreamEvent event) {
        return new SseGenerationEventPayload(
                event.generationId().toString(),
                event.status().name(),
                event.delta(),
                event.assistantMessageId() == null ? null : event.assistantMessageId().toString(),
                event.promptTokens(),
                event.completionTokens(),
                event.totalTokens(),
                event.errorCode()
        );
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "generationId invalido.");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "generationId invalido.", ex);
        }
    }
}
