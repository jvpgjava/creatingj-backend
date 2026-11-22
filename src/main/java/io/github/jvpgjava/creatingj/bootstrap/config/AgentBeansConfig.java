package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.agent.adapter.in.web.mapper.v1.AgentHttpMapper;
import io.github.jvpgjava.creatingj.agent.adapter.out.parser.JacksonAgentPlanParserAdapter;
import io.github.jvpgjava.creatingj.agent.application.port.in.CancelAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.CreateAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.DecideToolProposalUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.GetAgentRunUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.in.ListAgentRunsUseCase;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentPlanParserPort;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentToolProposalStorePort;
import io.github.jvpgjava.creatingj.agent.application.usecase.CancelAgentRunInteractor;
import io.github.jvpgjava.creatingj.agent.application.usecase.CreateAgentRunInteractor;
import io.github.jvpgjava.creatingj.agent.application.usecase.DecideToolProposalInteractor;
import io.github.jvpgjava.creatingj.agent.application.usecase.GetAgentRunInteractor;
import io.github.jvpgjava.creatingj.agent.application.usecase.ListAgentRunsInteractor;
import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.bootstrap.config.provider.AiProviderProperties;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.inference.application.port.in.EnforceQuotaAndRecordUsageUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.ModelGatewayPort;
import io.github.jvpgjava.creatingj.knowledge.application.port.in.SearchProjectKnowledgeUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class AgentBeansConfig {

    @Bean
    public AgentPlanParserPort agentPlanParserPort(ObjectMapper objectMapper) {
        return new JacksonAgentPlanParserAdapter(objectMapper);
    }

    @Bean
    public CreateAgentRunUseCase createAgentRunUseCase(
            ProjectStorePort projectStore,
            SearchProjectKnowledgeUseCase searchKnowledge,
            ModelGatewayPort modelGateway,
            AgentPlanParserPort planParser,
            EnforceQuotaAndRecordUsageUseCase quotaAndUsage,
            AgentRunStorePort runStore,
            AgentToolProposalStorePort proposalStore,
            AuditPort auditPort,
            ClockPort clock,
            AiProviderProperties providerProperties
    ) {
        return new CreateAgentRunInteractor(
                projectStore,
                searchKnowledge,
                modelGateway,
                planParser,
                quotaAndUsage,
                runStore,
                proposalStore,
                auditPort,
                clock,
                providerProperties.chatModel()
        );
    }

    @Bean
    public ListAgentRunsUseCase listAgentRunsUseCase(
            ProjectStorePort projectStore,
            AgentRunStorePort runStore
    ) {
        return new ListAgentRunsInteractor(projectStore, runStore);
    }

    @Bean
    public GetAgentRunUseCase getAgentRunUseCase(
            ProjectStorePort projectStore,
            AgentRunStorePort runStore,
            AgentToolProposalStorePort proposalStore
    ) {
        return new GetAgentRunInteractor(projectStore, runStore, proposalStore);
    }

    @Bean
    public DecideToolProposalUseCase decideToolProposalUseCase(
            ProjectStorePort projectStore,
            AgentRunStorePort runStore,
            AgentToolProposalStorePort proposalStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new DecideToolProposalInteractor(projectStore, runStore, proposalStore, auditPort, clock);
    }

    @Bean
    public CancelAgentRunUseCase cancelAgentRunUseCase(
            ProjectStorePort projectStore,
            AgentRunStorePort runStore,
            AgentToolProposalStorePort proposalStore,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new CancelAgentRunInteractor(projectStore, runStore, proposalStore, auditPort, clock);
    }

    @Bean
    public AgentHttpMapper agentHttpMapper() {
        return new AgentHttpMapper();
    }
}
