package io.github.jvpgjava.creatingj.knowledge.adapter.out.pgvector;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.HybridRagRanker;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunk;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.knowledge.domain.MultiQueryRagDecomposer;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class PgVectorKnowledgeStoreAdapter implements VectorKnowledgePort {

    private static final int CANDIDATES_PER_SIDE = 20;
    private static final double RELAXED_TEXT_SCORE_MARGIN = 0.20;

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
                        content_hash, token_estimate, embedding_version, embedding_dimensions, embedding, created_at
                    )
                    VALUES (
                        :id, :sourceId, :projectId, :ownerUserId, :chunkIndex, :content,
                        :contentHash, :tokenEstimate, :embeddingVersion, :embeddingDimensions,
                        CAST(:embedding AS vector), :createdAt
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
                    .param("embeddingDimensions", chunk.embedding().dimensions())
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
                  AND c.embedding_dimensions = :dimensions
                  AND (1.0 - (c.embedding <=> CAST(:queryVector AS vector))) >= :minScore
                ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
                LIMIT :topK
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .param("embeddingVersion", embeddingVersion)
                .param("dimensions", queryVector.dimensions())
                .param("queryVector", queryVector.toPgVectorLiteral())
                .param("minScore", minScore)
                .param("topK", topK)
                .query(this::mapMatch)
                .list();
    }

    @Override
    public List<KnowledgeSearchMatch> searchHybrid(
            ProjectId projectId,
            UserId ownerUserId,
            String queryText,
            EmbeddingVector queryVector,
            String embeddingVersion,
            int topK,
            double minScore
    ) {
        int candidateLimit = Math.max(CANDIDATES_PER_SIDE, topK * 2);
        List<KnowledgeSearchMatch> vectorCandidates = searchSimilar(
                projectId,
                ownerUserId,
                queryVector,
                embeddingVersion,
                candidateLimit,
                minScore
        );
        if (queryText == null || queryText.isBlank()) {
            return vectorCandidates.size() <= topK ? vectorCandidates : vectorCandidates.subList(0, topK);
        }

        List<KnowledgeSearchMatch> lexicalCandidates = queryFullText(
                projectId,
                ownerUserId,
                queryText,
                queryVector,
                embeddingVersion,
                minScore,
                candidateLimit
        );
        if (lexicalCandidates.isEmpty()) {
            String relaxedQuery = HybridRagRanker.buildRelaxedWebsearchQuery(queryText);
            if (!relaxedQuery.isBlank() && !relaxedQuery.equalsIgnoreCase(queryText.trim())) {
                double relaxedFloor = Math.max(-1.0, minScore - RELAXED_TEXT_SCORE_MARGIN);
                lexicalCandidates = queryFullText(
                        projectId,
                        ownerUserId,
                        relaxedQuery,
                        queryVector,
                        embeddingVersion,
                        relaxedFloor,
                        candidateLimit
                );
            }
        }

        List<KnowledgeSearchMatch> fused = HybridRagRanker.fuseVectorAndLexical(
                vectorCandidates,
                lexicalCandidates,
                topK,
                minScore
        );
        if (fused.isEmpty()) {
            return fused;
        }
        Map<KnowledgeSourceId, Integer> sourceChunkCounts = loadSourceChunkCounts(projectId, ownerUserId);
        List<KnowledgeSearchMatch> withSiblings = HybridRagRanker.expandSmallDocumentSiblings(
                fused,
                vectorCandidates,
                sourceChunkCounts,
                topK
        );
        return MultiQueryRagDecomposer.expandCutCodeNeighbors(withSiblings, vectorCandidates, topK);
    }

    private List<KnowledgeSearchMatch> queryFullText(
            ProjectId projectId,
            UserId ownerUserId,
            String websearchText,
            EmbeddingVector queryVector,
            String embeddingVersion,
            double minScoreFloor,
            int limit
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
                  AND c.embedding_dimensions = :dimensions
                  AND (
                      c.search_vector @@ websearch_to_tsquery('portuguese', :websearchText)
                      OR c.search_vector @@ websearch_to_tsquery('simple', :websearchText)
                  )
                  AND (1.0 - (c.embedding <=> CAST(:queryVector AS vector))) >= :minScoreFloor
                ORDER BY (
                    ts_rank_cd(c.search_vector, websearch_to_tsquery('portuguese', :websearchText))
                    + ts_rank_cd(c.search_vector, websearch_to_tsquery('simple', :websearchText))
                ) DESC,
                c.embedding <=> CAST(:queryVector AS vector) ASC
                LIMIT :limit
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .param("embeddingVersion", embeddingVersion)
                .param("dimensions", queryVector.dimensions())
                .param("queryVector", queryVector.toPgVectorLiteral())
                .param("websearchText", websearchText)
                .param("minScoreFloor", minScoreFloor)
                .param("limit", limit)
                .query(this::mapMatch)
                .list();
    }

    private Map<KnowledgeSourceId, Integer> loadSourceChunkCounts(ProjectId projectId, UserId ownerUserId) {
        Map<KnowledgeSourceId, Integer> counts = new HashMap<>();
        jdbcClient.sql("""
                SELECT id, chunk_count
                FROM knowledge_sources
                WHERE project_id = :projectId AND owner_user_id = :ownerUserId
                """)
                .param("projectId", projectId.value())
                .param("ownerUserId", ownerUserId.value())
                .query((rs, rowNum) -> {
                    counts.put(new KnowledgeSourceId(rs.getObject("id", UUID.class)), rs.getInt("chunk_count"));
                    return null;
                })
                .list();
        return counts;
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
