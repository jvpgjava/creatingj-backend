package io.github.jvpgjava.creatingj.knowledge.adapter.out.pgvector;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunk;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorKnowledgeStoreAdapter implements VectorKnowledgePort {

    private final JdbcClient jdbcClient;

    public PgVectorKnowledgeStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public void replaceSourceChunks(KnowledgeSourceId sourceId, List<KnowledgeChunk> chunks) {
        jdbcClient.sql("DELETE FROM knowledge_chunks WHERE source_id = :sourceId")
                .param("sourceId", sourceId.value())
                .update();

        for (KnowledgeChunk chunk : chunks) {
            jdbcClient.sql("""
                    INSERT INTO knowledge_chunks (
                        id, source_id, project_id, owner_user_id, chunk_index, content,
                        content_hash, token_estimate, embedding_version, embedding, created_at
                    )
                    VALUES (
                        :id, :sourceId, :projectId, :ownerUserId, :chunkIndex, :content,
                        :contentHash, :tokenEstimate, :embeddingVersion, CAST(:embedding AS vector), :createdAt
                    )
                    """)
                    .param("id", chunk.id().value())
                    .param("sourceId", chunk.sourceId().value())
                    .param("projectId", chunk.projectId().value())
                    .param("ownerUserId", chunk.ownerUserId().value())
                    .param("chunkIndex", chunk.chunkIndex())
                    .param("content", chunk.content())
                    .param("contentHash", chunk.contentHash())
                    .param("tokenEstimate", chunk.tokenEstimate())
                    .param("embeddingVersion", chunk.embeddingVersion())
                    .param("embedding", chunk.embedding().toPgVectorLiteral())
                    .param("createdAt", OffsetDateTime.ofInstant(chunk.createdAt(), ZoneOffset.UTC))
                    .update();
        }
    }

    @Override
    public List<KnowledgeSearchMatch> searchSimilar(
            ProjectId projectId,
            UserId ownerUserId,
            EmbeddingVector queryVector,
            String embeddingVersion,
            int topK,
            double minScore
    ) {
        return jdbcClient.sql("""
                SELECT c.source_id,
                       s.source_path,
                       s.title,
                       c.chunk_index,
                       c.content,
                       (1.0 - (c.embedding <=> CAST(:queryVector AS vector))) AS similarity_score,
                       c.embedding_version
                FROM knowledge_chunks c
                INNER JOIN knowledge_sources s ON s.id = c.source_id
                WHERE c.project_id = :projectId
                  AND c.owner_user_id = :ownerUserId
                  AND s.owner_user_id = :ownerUserId
                  AND c.embedding_version = :embeddingVersion
                  AND (1.0 - (c.embedding <=> CAST(:queryVector AS vector))) >= :minScore
                ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
                LIMIT :topK
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .param("embeddingVersion", embeddingVersion)
                .param("queryVector", queryVector.toPgVectorLiteral())
                .param("minScore", minScore)
                .param("topK", topK)
                .query(this::mapMatch)
                .list();
    }

    @Override
    public void deleteBySource(KnowledgeSourceId sourceId) {
        jdbcClient.sql("DELETE FROM knowledge_chunks WHERE source_id = :sourceId")
                .param("sourceId", sourceId.value())
                .update();
    }

    @Override
    public void deleteByProject(ProjectId projectId, UserId ownerUserId) {
        jdbcClient.sql("""
                DELETE FROM knowledge_chunks
                WHERE project_id = :projectId AND owner_user_id = :ownerUserId
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .update();
    }

    private KnowledgeSearchMatch mapMatch(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeSearchMatch(
                new KnowledgeSourceId(rs.getObject("source_id", UUID.class)),
                rs.getString("source_path"),
                rs.getString("title"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("similarity_score"),
                rs.getString("embedding_version")
        );
    }
}
