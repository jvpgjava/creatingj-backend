package io.github.jvpgjava.creatingj.agent.application.port.out;

import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.agent.domain.ToolProposalId;

import java.util.List;
import java.util.Optional;

public interface AgentToolProposalStorePort {

    AgentToolProposal save(AgentToolProposal proposal);

    List<AgentToolProposal> saveAll(List<AgentToolProposal> proposals);

    Optional<AgentToolProposal> findById(ToolProposalId proposalId);

    List<AgentToolProposal> findByRunOrderByStepAsc(AgentRunId runId);
}
