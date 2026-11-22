package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.bootstrap.config.provider.AiProviderProperties;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CancelGenerationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.CreateConversationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.GetConversationHistoryUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.ListConversationsUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.SendMessageUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.StreamMessageGenerationUseCase;
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
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.ModelGatewayPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConversationBeansConfig {

    @Bean
    public CreateConversationUseCase createConversationUseCase(
            ConversationStorePort conversationStore,
            AuditPort auditPort,
            ClockPort clockPort,
            AiProviderProperties providerProperties
    ) {
        return new CreateConversationInteractor(
                conversationStore, auditPort, clockPort, providerProperties.chatModel());
    }

    @Bean
    public ListConversationsUseCase listConversationsUseCase(ConversationStorePort conversationStore) {
        return new ListConversationsInteractor(conversationStore);
    }

    @Bean
    public GetConversationHistoryUseCase getConversationHistoryUseCase(
            ConversationStorePort conversationStore,
            MessageStorePort messageStore,
            GenerationStorePort generationStore
    ) {
        return new GetConversationHistoryInteractor(conversationStore, messageStore, generationStore);
    }

    @Bean
    public SendMessageUseCase sendMessageUseCase(
            ConversationStorePort conversationStore,
            MessageStorePort messageStore,
            GenerationStorePort generationStore,
            ModelGatewayPort modelGateway,
            EnforceQuotaAndRecordUsageUseCase quotaUsage,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.provider.ai.default-max-tokens:2048}") int defaultMaxTokens
    ) {
        return new SendMessageInteractor(
                conversationStore, messageStore, generationStore, modelGateway, quotaUsage,
                auditPort, clockPort, defaultMaxTokens);
    }

    @Bean
    public StreamMessageGenerationUseCase streamMessageGenerationUseCase(
            ConversationStorePort conversationStore,
            MessageStorePort messageStore,
            GenerationStorePort generationStore,
            ActiveGenerationRegistryPort activeRegistry,
            ModelGatewayPort modelGateway,
            EnforceQuotaAndRecordUsageUseCase quotaUsage,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.provider.ai.default-max-tokens:2048}") int defaultMaxTokens
    ) {
        return new StreamMessageGenerationInteractor(
                conversationStore, messageStore, generationStore, activeRegistry, modelGateway, quotaUsage,
                auditPort, clockPort, defaultMaxTokens);
    }

    @Bean
    public CancelGenerationUseCase cancelGenerationUseCase(
            ConversationStorePort conversationStore,
            GenerationStorePort generationStore,
            ActiveGenerationRegistryPort activeRegistry,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new CancelGenerationInteractor(
                conversationStore, generationStore, activeRegistry, auditPort, clockPort);
    }
}
