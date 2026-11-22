package io.github.jvpgjava.creatingj.task.adapter.out.persistence;

import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import io.github.jvpgjava.creatingj.task.application.port.out.RemoteTaskStorePort;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.TaskStatus;
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
public class JdbcRemoteTaskStoreAdapter implements RemoteTaskStorePort {

    private final JdbcClient jdbcClient;

    public JdbcRemoteTaskStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public RemoteTask save(RemoteTask task) {
        jdbcClient.sql("""
                INSERT INTO remote_tasks (
                    id, owner_user_id, project_id, device_id, idempotency_key, title, instruction,
                    status, lease_expires_at, result_summary, error_code, created_at, updated_at, completed_at
                )
                VALUES (
                    :id, :ownerUserId, :projectId, :deviceId, :idempotencyKey, :title, :instruction,
                    :status, :leaseExpiresAt, :resultSummary, :errorCode, :createdAt, :updatedAt, :completedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    lease_expires_at = EXCLUDED.lease_expires_at,
                    result_summary = EXCLUDED.result_summary,
                    error_code = EXCLUDED.error_code,
                    updated_at = EXCLUDED.updated_at,
                    completed_at = EXCLUDED.completed_at
                """)
                .param("id", task.id().value())
                .param("ownerUserId", task.ownerUserId().value())
                .param("projectId", task.projectId() == null ? null : task.projectId().value())
                .param("deviceId", task.deviceId().value())
                .param("idempotencyKey", task.idempotencyKey())
                .param("title", task.title())
                .param("instruction", task.instruction())
                .param("status", task.status().name())
                .param("leaseExpiresAt", toOffset(task.leaseExpiresAt()))
                .param("resultSummary", task.resultSummary())
                .param("errorCode", task.errorCode())
                .param("createdAt", toOffset(task.createdAt()))
                .param("updatedAt", toOffset(task.updatedAt()))
                .param("completedAt", toOffset(task.completedAt()))
                .update();
        return task;
    }

    @Override
    public Optional<RemoteTask> findById(TaskId taskId) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, project_id, device_id, idempotency_key, title, instruction,
                       status, lease_expires_at, result_summary, error_code, created_at, updated_at, completed_at
                FROM remote_tasks
                WHERE id = :id
                """)
                .param("id", taskId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<RemoteTask> findByIdempotency(UserId ownerUserId, DeviceId deviceId, String idempotencyKey) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, project_id, device_id, idempotency_key, title, instruction,
                       status, lease_expires_at, result_summary, error_code, created_at, updated_at, completed_at
                FROM remote_tasks
                WHERE owner_user_id = :ownerUserId
                  AND device_id = :deviceId
                  AND idempotency_key = :idempotencyKey
                """)
                .param("ownerUserId", ownerUserId.value())
                .param("deviceId", deviceId.value())
                .param("idempotencyKey", idempotencyKey)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<RemoteTask> findRecentByOwner(UserId ownerUserId, int limit) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, project_id, device_id, idempotency_key, title, instruction,
                       status, lease_expires_at, result_summary, error_code, created_at, updated_at, completed_at
                FROM remote_tasks
                WHERE owner_user_id = :ownerUserId
                ORDER BY created_at DESC
                LIMIT :limit
                """)
                .param("ownerUserId", ownerUserId.value())
                .param("limit", limit)
                .query(this::mapRow)
                .list();
    }

    @Override
    public Optional<RemoteTask> findNextLeasableForDevice(DeviceId deviceId, Instant now) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, project_id, device_id, idempotency_key, title, instruction,
                       status, lease_expires_at, result_summary, error_code, created_at, updated_at, completed_at
                FROM remote_tasks
                WHERE device_id = :deviceId
                  AND (
                      status = 'QUEUED'
                      OR (status IN ('LEASED', 'RUNNING') AND lease_expires_at <= :now)
                  )
                ORDER BY created_at ASC
                LIMIT 1
                """)
                .param("deviceId", deviceId.value())
                .param("now", toOffset(now))
                .query(this::mapRow)
                .optional();
    }

    private RemoteTask mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID projectUuid = rs.getObject("project_id", UUID.class);
        return new RemoteTask(
                new TaskId(rs.getObject("id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                projectUuid == null ? null : new ProjectId(projectUuid),
                new DeviceId(rs.getObject("device_id", UUID.class)),
                rs.getString("idempotency_key"),
                rs.getString("title"),
                rs.getString("instruction"),
                TaskStatus.valueOf(rs.getString("status")),
                toInstant(rs.getObject("lease_expires_at", OffsetDateTime.class)),
                rs.getString("result_summary"),
                rs.getString("error_code"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                toInstant(rs.getObject("completed_at", OffsetDateTime.class))
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
