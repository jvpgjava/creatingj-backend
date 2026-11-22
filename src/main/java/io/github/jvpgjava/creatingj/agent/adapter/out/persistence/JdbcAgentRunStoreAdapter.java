package io.github.jvpgjava.creatingj.agent.adapter.out.persistence;

import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunStatus;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAgentRunStoreAdapter implements AgentRunStorePort {

    private final JdbcClient jdbcClient;

    public JdbcAgentRunStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public AgentRun save(AgentRun run) {
        jdbcClient.sql("""
                INSERT INTO agent_runs (
                    id, project_id, owner_user_id, goal, model_id, plan_summary,
                    status, workspace_file_count, rag_match_count, created_at, updated_at
                )
                VALUES (
                    :id, :projectId, :ownerUserId, :goal, :modelId, :planSummary,
                    :status, :workspaceFileCount, :ragMatchCount, :createdAt, :updatedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    plan_summary = EXCLUDED.plan_summary,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """)
                .param("id", run.id().value())
                .param("projectId", run.projectId().value())
                .param("ownerUserId", run.ownerUserId().value())
                .param("goal", run.goal())
                .param("modelId", run.modelId())
                .param("planSummary", run.planSummary())
                .param("status", run.status().name())
                .param("workspaceFileCount", run.workspaceFileCount())
                .param("ragMatchCount", run.ragMatchCount())
                .param("createdAt", OffsetDateTime.ofInstant(run.createdAt(), ZoneOffset.UTC))
                .param("updatedAt", OffsetDateTime.ofInstant(run.updatedAt(), ZoneOffset.UTC))
                .update();
        return run;
    }

    @Override
    public Optional<AgentRun> findById(AgentRunId runId) {
        return jdbcClient.sql("""
                SELECT id, project_id, owner_user_id, goal, model_id, plan_summary,
                       status, workspace_file_count, rag_match_count, created_at, updated_at
                FROM agent_runs
                WHERE id = :id
                """)
                .param("id", runId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<AgentRun> findRecentByProjectAndOwner(ProjectId projectId, UserId ownerUserId, int limit) {
        return jdbcClient.sql("""
                SELECT id, project_id, owner_user_id, goal, model_id, plan_summary,
                       status, workspace_file_count, rag_match_count, created_at, updated_at
                FROM agent_runs
                WHERE project_id = :projectId AND owner_user_id = :ownerUserId
                ORDER BY created_at DESC
                LIMIT :limit
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .param("limit", limit)
                .query(this::mapRow)
                .list();
    }

    private AgentRun mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AgentRun(
                new AgentRunId(rs.getObject("id", UUID.class)),
                new ProjectId(rs.getObject("project_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("goal"),
                rs.getString("model_id"),
                rs.getString("plan_summary"),
                AgentRunStatus.valueOf(rs.getString("status")),
                rs.getInt("workspace_file_count"),
                rs.getInt("rag_match_count"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
