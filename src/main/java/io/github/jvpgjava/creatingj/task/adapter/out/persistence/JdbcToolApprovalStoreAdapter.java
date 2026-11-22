package io.github.jvpgjava.creatingj.task.adapter.out.persistence;

import io.github.jvpgjava.creatingj.agent.domain.AgentToolName;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.task.application.port.out.ToolApprovalStorePort;
import io.github.jvpgjava.creatingj.task.domain.RemoteApprovalStatus;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
public class JdbcToolApprovalStoreAdapter implements ToolApprovalStorePort {

    private final JdbcClient jdbcClient;

    public JdbcToolApprovalStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ToolApproval save(ToolApproval approval) {
        jdbcClient.sql("""
                INSERT INTO tool_approvals (
                    id, task_id, owner_user_id, device_id, tool_name, target_path,
                    command_text, diff_preview, status, expires_at, decided_at, created_at
                )
                VALUES (
                    :id, :taskId, :ownerUserId, :deviceId, :toolName, :targetPath,
                    :commandText, :diffPreview, :status, :expiresAt, :decidedAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    decided_at = EXCLUDED.decided_at
                """)
                .param("id", approval.id().value())
                .param("taskId", approval.taskId().value())
                .param("ownerUserId", approval.ownerUserId().value())
                .param("deviceId", approval.deviceId().value())
                .param("toolName", approval.toolName().name())
                .param("targetPath", approval.targetPath())
                .param("commandText", approval.commandText())
                .param("diffPreview", approval.diffPreview())
                .param("status", approval.status().name())
                .param("expiresAt", OffsetDateTime.ofInstant(approval.expiresAt(), ZoneOffset.UTC))
                .param("decidedAt", approval.decidedAt() == null
                        ? null
                        : OffsetDateTime.ofInstant(approval.decidedAt(), ZoneOffset.UTC))
                .param("createdAt", OffsetDateTime.ofInstant(approval.createdAt(), ZoneOffset.UTC))
                .update();
        return approval;
    }

    @Override
    public Optional<ToolApproval> findById(ToolApprovalId approvalId) {
        return jdbcClient.sql("""
                SELECT id, task_id, owner_user_id, device_id, tool_name, target_path,
                       command_text, diff_preview, status, expires_at, decided_at, created_at
                FROM tool_approvals
                WHERE id = :id
                """)
                .param("id", approvalId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<ToolApproval> findByTaskOrderByCreatedAsc(TaskId taskId) {
        return jdbcClient.sql("""
                SELECT id, task_id, owner_user_id, device_id, tool_name, target_path,
                       command_text, diff_preview, status, expires_at, decided_at, created_at
                FROM tool_approvals
                WHERE task_id = :taskId
                ORDER BY created_at ASC
                """)
                .param("taskId", taskId.value())
                .query(this::mapRow)
                .list();
    }

    private ToolApproval mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime decidedAt = rs.getObject("decided_at", OffsetDateTime.class);
        return new ToolApproval(
                new ToolApprovalId(rs.getObject("id", UUID.class)),
                new TaskId(rs.getObject("task_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                new DeviceId(rs.getObject("device_id", UUID.class)),
                AgentToolName.valueOf(rs.getString("tool_name")),
                rs.getString("target_path"),
                rs.getString("command_text"),
                rs.getString("diff_preview"),
                RemoteApprovalStatus.valueOf(rs.getString("status")),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                decidedAt == null ? (Instant) null : decidedAt.toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
