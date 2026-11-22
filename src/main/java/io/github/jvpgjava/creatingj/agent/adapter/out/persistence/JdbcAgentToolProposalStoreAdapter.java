package io.github.jvpgjava.creatingj.agent.adapter.out.persistence;

import io.github.jvpgjava.creatingj.agent.application.port.out.AgentToolProposalStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolName;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.agent.domain.ToolApprovalStatus;
import io.github.jvpgjava.creatingj.agent.domain.ToolPolicyDecision;
import io.github.jvpgjava.creatingj.agent.domain.ToolProposalId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAgentToolProposalStoreAdapter implements AgentToolProposalStorePort {

    private final JdbcClient jdbcClient;

    public JdbcAgentToolProposalStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public AgentToolProposal save(AgentToolProposal proposal) {
        jdbcClient.sql("""
                INSERT INTO agent_tool_proposals (
                    id, run_id, project_id, owner_user_id, step_index, tool_name,
                    target_path, command_text, rationale, policy_decision, policy_reason,
                    approval_status, decided_at, created_at
                )
                VALUES (
                    :id, :runId, :projectId, :ownerUserId, :stepIndex, :toolName,
                    :targetPath, :commandText, :rationale, :policyDecision, :policyReason,
                    :approvalStatus, :decidedAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    approval_status = EXCLUDED.approval_status,
                    decided_at = EXCLUDED.decided_at
                """)
                .param("id", proposal.id().value())
                .param("runId", proposal.runId().value())
                .param("projectId", proposal.projectId().value())
                .param("ownerUserId", proposal.ownerUserId().value())
                .param("stepIndex", proposal.stepIndex())
                .param("toolName", proposal.toolName().name())
                .param("targetPath", proposal.targetPath())
                .param("commandText", proposal.commandText())
                .param("rationale", proposal.rationale())
                .param("policyDecision", proposal.policyDecision().name())
                .param("policyReason", proposal.policyReason())
                .param("approvalStatus", proposal.approvalStatus().name())
                .param("decidedAt", proposal.decidedAt() == null
                        ? null
                        : OffsetDateTime.ofInstant(proposal.decidedAt(), ZoneOffset.UTC))
                .param("createdAt", OffsetDateTime.ofInstant(proposal.createdAt(), ZoneOffset.UTC))
                .update();
        return proposal;
    }

    @Override
    @Transactional
    public List<AgentToolProposal> saveAll(List<AgentToolProposal> proposals) {
        for (AgentToolProposal proposal : proposals) {
            save(proposal);
        }
        return proposals;
    }

    @Override
    public Optional<AgentToolProposal> findById(ToolProposalId proposalId) {
        return jdbcClient.sql("""
                SELECT id, run_id, project_id, owner_user_id, step_index, tool_name,
                       target_path, command_text, rationale, policy_decision, policy_reason,
                       approval_status, decided_at, created_at
                FROM agent_tool_proposals
                WHERE id = :id
                """)
                .param("id", proposalId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<AgentToolProposal> findByRunOrderByStepAsc(AgentRunId runId) {
        return jdbcClient.sql("""
                SELECT id, run_id, project_id, owner_user_id, step_index, tool_name,
                       target_path, command_text, rationale, policy_decision, policy_reason,
                       approval_status, decided_at, created_at
                FROM agent_tool_proposals
                WHERE run_id = :runId
                ORDER BY step_index ASC
                """)
                .param("runId", runId.value())
                .query(this::mapRow)
                .list();
    }

    private AgentToolProposal mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime decidedAt = rs.getObject("decided_at", OffsetDateTime.class);
        return new AgentToolProposal(
                new ToolProposalId(rs.getObject("id", UUID.class)),
                new AgentRunId(rs.getObject("run_id", UUID.class)),
                new ProjectId(rs.getObject("project_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getInt("step_index"),
                AgentToolName.valueOf(rs.getString("tool_name")),
                rs.getString("target_path"),
                rs.getString("command_text"),
                rs.getString("rationale"),
                ToolPolicyDecision.valueOf(rs.getString("policy_decision")),
                rs.getString("policy_reason"),
                ToolApprovalStatus.valueOf(rs.getString("approval_status")),
                decidedAt == null ? (Instant) null : decidedAt.toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
