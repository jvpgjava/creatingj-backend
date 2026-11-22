package io.github.jvpgjava.creatingj.conversation.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.conversation.adapter.in.web.apidocs.v1.ConversationApiDocs;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationHistoryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationSummaryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationsPageResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.CreateConversationRequest;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.GenerationSummaryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.MessageExchangeResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.SendMessageRequest;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.mapper.v1.ConversationHttpMapper;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.streaming.SseGenerationBridge;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CancelGenerationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CreateConversationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.GetConversationHistoryUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.ListConversationsUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.SendMessageUseCase;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/conversations")
public final class ConversationController implements ConversationApiDocs {

    private final CreateConversationUseCase createConversation;
    private final ListConversationsUseCase listConversations;
    private final GetConversationHistoryUseCase getConversationHistory;
    private final SendMessageUseCase sendMessage;
    private final CancelGenerationUseCase cancelGeneration;
    private final SseGenerationBridge sseBridge;
    private final ConversationHttpMapper mapper;

    public ConversationController(
            CreateConversationUseCase createConversation,
            ListConversationsUseCase listConversations,
            GetConversationHistoryUseCase getConversationHistory,
            SendMessageUseCase sendMessage,
            CancelGenerationUseCase cancelGeneration,
            SseGenerationBridge sseBridge,
            ConversationHttpMapper mapper
    ) {
        this.createConversation = createConversation;
        this.listConversations = listConversations;
        this.getConversationHistory = getConversationHistory;
        this.sendMessage = sendMessage;
        this.cancelGeneration = cancelGeneration;
        this.sseBridge = sseBridge;
        this.mapper = mapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<ConversationSummaryResponse> create(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var created = createConversation.execute(mapper.toCreateCommand(actor.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toSummary(created));
    }

    @Override
    @GetMapping
    public ResponseEntity<ConversationsPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return ResponseEntity.ok(mapper.toPage(listConversations.execute(mapper.toListQuery(actor.id(), page, size))));
    }

    @Override
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationHistoryResponse> history(
            @PathVariable String conversationId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = getConversationHistory.execute(actor.id(), ConversationId.fromString(conversationId));
        return ResponseEntity.ok(mapper.toHistory(result));
    }

    @Override
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageExchangeResponse> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = sendMessage.execute(mapper.toSendMessageCommand(actor.id(), conversationId, request));
        return ResponseEntity.ok(mapper.toExchange(result));
    }

    @Override
    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return sseBridge.openStream(mapper.toStreamCommand(actor.id(), conversationId, request));
    }

    @Override
    @PostMapping("/{conversationId}/generations/{generationId}/cancel")
    public ResponseEntity<GenerationSummaryResponse> cancelGeneration(
            @PathVariable String conversationId,
            @PathVariable String generationId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = cancelGeneration.execute(mapper.toCancelCommand(actor.id(), conversationId, generationId));
        return ResponseEntity.ok(mapper.toGeneration(result));
    }
}
