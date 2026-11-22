package io.github.jvpgjava.creatingj.task.adapter.out.persistence;

import io.github.jvpgjava.creatingj.task.application.port.out.TaskEventStorePort;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskEventType;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
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
public class JdbcTaskEventStoreAdapter implements TaskEventStorePort {

    private final JdbcClient jdbcClient;

    public JdbcTaskEventStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<TaskEvent> findByTaskAndEventKey(TaskId taskId, String eventKey) {
        return jdbcClient.sql("""
                SELECT id, task_id, event_key, event_type, message, created_at
                FROM task_events
                WHERE task_id = :taskId AND event_key = :eventKey
                """)
                .param("taskId", taskId.value())
                .param("eventKey", eventKey)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public TaskEvent save(TaskEvent event) {
        jdbcClient.sql("""
                INSERT INTO task_events (id, task_id, event_key, event_type, message, created_at)
                VALUES (:id, :taskId, :eventKey, :eventType, :message, :createdAt)
                ON CONFLICT (task_id, event_key) DO NOTHING
                """)
                .param("id", event.id())
                .param("taskId", event.taskId().value())
                .param("eventKey", event.eventKey())
                .param("eventType", event.eventType().name())
                .param("message", event.message())
                .param("createdAt", OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC))
                .update();
        return event;
    }

    @Override
    public List<TaskEvent> findByTaskOrderByCreatedAsc(TaskId taskId) {
        return jdbcClient.sql("""
                SELECT id, task_id, event_key, event_type, message, created_at
                FROM task_events
                WHERE task_id = :taskId
                ORDER BY created_at ASC
                """)
                .param("taskId", taskId.value())
                .query(this::mapRow)
                .list();
    }

    private TaskEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TaskEvent(
                rs.getObject("id", UUID.class),
                new TaskId(rs.getObject("task_id", UUID.class)),
                rs.getString("event_key"),
                TaskEventType.valueOf(rs.getString("event_type")),
                rs.getString("message"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
