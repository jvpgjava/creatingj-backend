package io.github.jvpgjava.creatingj.conversation.application.port.in;

import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;

public interface SendMessageUseCase {

    MessageExchangeResult execute(SendMessageCommand command);

    record SendMessageCommand(
            UserId actorUserId,
            ConversationId conversationId,
            String content
    ) {
    }

    record MessageExchangeResult(
            ChatMessage userMessage,
            ChatMessage assistantMessage,
            Generation generation,
            List<String> followUpSuggestions,
            List<String> guardrailFlags
    ) {

        public MessageExchangeResult {
            followUpSuggestions = followUpSuggestions == null ? List.of() : List.copyOf(followUpSuggestions);
            guardrailFlags = guardrailFlags == null ? List.of() : List.copyOf(guardrailFlags);
        }

        public MessageExchangeResult(
                ChatMessage userMessage,
                ChatMessage assistantMessage,
                Generation generation
        ) {
            this(userMessage, assistantMessage, generation, List.of(), List.of());
        }
    }
}
