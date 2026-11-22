package io.github.jvpgjava.creatingj.knowledge.adapter.out.pgvector;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceStatus;
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
public class JdbcKnowledgeSourceStoreAdapter implements KnowledgeSourceStorePort {

    private final JdbcClient jdbcClient;

    public JdbcKnowledgeSourceStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public KnowledgeSource save(KnowledgeSource source) {
        jdbcClient.sql("""
                INSERT INTO knowledge_sources (
                    id, project_id, owner_user_id, source_path, title, content_hash,
                    embedding_version, status, chunk_count, created_at, updated_at
                )
                VALUES (
                    :id, :projectId, :ownerUserId, :sourcePath, :title, :contentHash,
                    :embeddingVersion, :status, :chunkCount, :createdAt, :updatedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    content_hash = EXCLUDED.content_hash,
                    embedding_version = EXCLUDED.embedding_version,
                    status = EXCLUDED.status,
                    chunk_count = EXCLUDED.chunk_count,
                    updated_at = EXCLUDED.updated_at
                """)
                .param("id", source.id().value())
                .param("projectId", source.projectId().value())
                .param("ownerUserId", source.ownerUserId().value())
                .param("sourcePath", source.sourcePath())
                .param("title", source.title())
                .param("contentHash", source.contentHash())
                .param("embeddingVersion", source.embeddingVersion())
                .param("status", source.status().name())
                .param("chunkCount", source.chunkCount())
                .param("createdAt", OffsetDateTime.ofInstant(source.createdAt(), ZoneOffset.UTC))
                .param("updatedAt", OffsetDateTime.ofInstant(source.updatedAt(), ZoneOffset.UTC))
                .update();
        return source;
    }

    @Override
    public Optional<KnowledgeSource> findById(KnowledgeSourceId sourceId) {
        return jdbcClient.sql("""
                SELECT id, project_id, owner_user_id, source_path, title, content_hash,
                       embedding_version, status, chunk_count, created_at, updated_at
                FROM knowledge_sources
                WHERE id = :id
                """)
                .param("id", sourceId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<KnowledgeSource> findByProjectAndPath(ProjectId projectId, String sourcePath) {
        return jdbcClient.sql("""
                SELECT id, project_id, owner_user_id, source_path, title, content_hash,
                       embedding_version, status, chunk_count, created_at, updated_at
                FROM knowledge_sources
                WHERE project_id = :projectId AND source_path = :sourcePath
                """)
                .param("projectId", projectId.value())
                .param("sourcePath", sourcePath)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<KnowledgeSource> findByProjectAndOwner(ProjectId projectId, UserId ownerUserId) {
        return jdbcClient.sql("""
                SELECT id, project_id, owner_user_id, source_path, title, content_hash,
                       embedding_version, status, chunk_count, created_at, updated_at
                FROM knowledge_sources
                WHERE project_id = :projectId AND owner_user_id = :ownerUserId
                ORDER BY updated_at DESC
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public void deleteById(KnowledgeSourceId sourceId) {
        jdbcClient.sql("DELETE FROM knowledge_sources WHERE id = :id")
                .param("id", sourceId.value())
                .update();
    }

    @Override
    public void deleteByProject(ProjectId projectId, UserId ownerUserId) {
        jdbcClient.sql("""
                DELETE FROM knowledge_sources
                WHERE project_id = :projectId AND owner_user_id = :ownerUserId
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .update();
    }

    private KnowledgeSource mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeSource(
                new KnowledgeSourceId(rs.getObject("id", UUID.class)),
                new ProjectId(rs.getObject("project_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("source_path"),
                rs.getString("title"),
                rs.getString("content_hash"),
                rs.getString("embedding_version"),
                KnowledgeSourceStatus.valueOf(rs.getString("status")),
                rs.getInt("chunk_count"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
