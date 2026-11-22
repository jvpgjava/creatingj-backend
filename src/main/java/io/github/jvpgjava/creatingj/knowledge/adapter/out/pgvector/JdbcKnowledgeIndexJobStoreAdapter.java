package io.github.jvpgjava.creatingj.knowledge.adapter.out.pgvector;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeIndexJobStorePort;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJobId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeJobOperation;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeJobStatus;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcKnowledgeIndexJobStoreAdapter implements KnowledgeIndexJobStorePort {

    private final JdbcClient jdbcClient;

    public JdbcKnowledgeIndexJobStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public KnowledgeIndexJob save(KnowledgeIndexJob job) {
        jdbcClient.sql("""
                INSERT INTO knowledge_index_jobs (
                    id, project_id, owner_user_id, source_id, source_path, operation,
                    status, chunks_indexed, error_code, created_at, completed_at
                )
                VALUES (
                    :id, :projectId, :ownerUserId, :sourceId, :sourcePath, :operation,
                    :status, :chunksIndexed, :errorCode, :createdAt, :completedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    source_id = EXCLUDED.source_id,
                    status = EXCLUDED.status,
                    chunks_indexed = EXCLUDED.chunks_indexed,
                    error_code = EXCLUDED.error_code,
                    completed_at = EXCLUDED.completed_at
                """)
                .param("id", job.id().value())
                .param("projectId", job.projectId().value())
                .param("ownerUserId", job.ownerUserId().value())
                .param("sourceId", job.sourceId() == null ? null : job.sourceId().value())
                .param("sourcePath", job.sourcePath())
                .param("operation", job.operation().name())
                .param("status", job.status().name())
                .param("chunksIndexed", job.chunksIndexed())
                .param("errorCode", job.errorCode())
                .param("createdAt", OffsetDateTime.ofInstant(job.createdAt(), ZoneOffset.UTC))
                .param("completedAt", job.completedAt() == null
                        ? null
                        : OffsetDateTime.ofInstant(job.completedAt(), ZoneOffset.UTC))
                .update();
        return job;
    }

    @Override
    public List<KnowledgeIndexJob> findRecentByProjectAndOwner(ProjectId projectId, UserId ownerUserId, int limit) {
        return jdbcClient.sql("""
                SELECT id, project_id, owner_user_id, source_id, source_path, operation,
                       status, chunks_indexed, error_code, created_at, completed_at
                FROM knowledge_index_jobs
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

    private KnowledgeIndexJob mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID sourceUuid = rs.getObject("source_id", UUID.class);
        OffsetDateTime completedAt = rs.getObject("completed_at", OffsetDateTime.class);
        return new KnowledgeIndexJob(
                new KnowledgeIndexJobId(rs.getObject("id", UUID.class)),
                new ProjectId(rs.getObject("project_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                sourceUuid == null ? null : new KnowledgeSourceId(sourceUuid),
                rs.getString("source_path"),
                KnowledgeJobOperation.valueOf(rs.getString("operation")),
                KnowledgeJobStatus.valueOf(rs.getString("status")),
                rs.getInt("chunks_indexed"),
                rs.getString("error_code"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                completedAt == null ? (Instant) null : completedAt.toInstant()
        );
    }
}
