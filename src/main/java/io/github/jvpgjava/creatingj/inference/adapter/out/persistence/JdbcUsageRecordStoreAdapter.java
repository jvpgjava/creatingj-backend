package io.github.jvpgjava.creatingj.inference.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.domain.UsageOutcome;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UsageSource;
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
public class JdbcUsageRecordStoreAdapter implements UsageRecordStorePort {

    private final JdbcClient jdbcClient;

    public JdbcUsageRecordStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UsageRecord save(UsageRecord record) {
        jdbcClient.sql("""
                        INSERT INTO usage_records
                            (id, user_id, conversation_id, generation_id, model_id, source,
                             prompt_tokens, completion_tokens, total_tokens, estimated,
                             latency_ms, outcome, recorded_at)
                        VALUES
                            (:id, :userId, :conversationId, :generationId, :modelId, :source,
                             :promptTokens, :completionTokens, :totalTokens, :estimated,
                             :latencyMs, :outcome, :recordedAt)
                        """)
                .param("id", record.id())
                .param("userId", record.userId().value())
                .param("conversationId", record.conversationId())
                .param("generationId", record.generationId())
                .param("modelId", record.modelId())
                .param("source", record.source().name())
                .param("promptTokens", record.promptTokens())
                .param("completionTokens", record.completionTokens())
                .param("totalTokens", record.totalTokens())
                .param("estimated", record.estimated())
                .param("latencyMs", record.latencyMs())
                .param("outcome", record.outcome().name())
                .param("recordedAt", toOffset(record.recordedAt()))
                .update();
        return record;
    }

    @Override
    public long sumTokensByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive) {
        Long sum = jdbcClient.sql("""
                        SELECT COALESCE(SUM(total_tokens), 0)
                        FROM usage_records
                        WHERE user_id = :userId
                          AND recorded_at >= :start
                          AND recorded_at < :end
                        """)
                .param("userId", userId.value())
                .param("start", toOffset(startInclusive))
                .param("end", toOffset(endExclusive))
                .query(Long.class)
                .single();
        return sum == null ? 0L : sum;
    }

    @Override
    public long countRequestsByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive) {
        Long count = jdbcClient.sql("""
                        SELECT count(*)
                        FROM usage_records
                        WHERE user_id = :userId
                          AND recorded_at >= :start
                          AND recorded_at < :end
                        """)
                .param("userId", userId.value())
                .param("start", toOffset(startInclusive))
                .param("end", toOffset(endExclusive))
                .query(Long.class)
                .single();
        return count == null ? 0L : count;
    }

    @Override
    public List<UsageRecord> findRecentByUser(UserId userId, int limit) {
        return jdbcClient.sql("""
                        SELECT id, user_id, conversation_id, generation_id, model_id, source,
                               prompt_tokens, completion_tokens, total_tokens, estimated,
                               latency_ms, outcome, recorded_at
                        FROM usage_records
                        WHERE user_id = :userId
                        ORDER BY recorded_at DESC
                        LIMIT :limit
                        """)
                .param("userId", userId.value())
                .param("limit", limit)
                .query(this::mapRow)
                .list();
    }

    @Override
    public long sumAllTokensBetween(Instant startInclusive, Instant endExclusive) {
        Long sum = jdbcClient.sql("""
                        SELECT COALESCE(SUM(total_tokens), 0)
                        FROM usage_records
                        WHERE recorded_at >= :start
                          AND recorded_at < :end
                        """)
                .param("start", toOffset(startInclusive))
                .param("end", toOffset(endExclusive))
                .query(Long.class)
                .single();
        return sum == null ? 0L : sum;
    }

    @Override
    public long countAllRequestsBetween(Instant startInclusive, Instant endExclusive) {
        Long count = jdbcClient.sql("""
                        SELECT count(*)
                        FROM usage_records
                        WHERE recorded_at >= :start
                          AND recorded_at < :end
                        """)
                .param("start", toOffset(startInclusive))
                .param("end", toOffset(endExclusive))
                .query(Long.class)
                .single();
        return count == null ? 0L : count;
    }

    private UsageRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UsageRecord(
                rs.getObject("id", UUID.class),
                new UserId(rs.getObject("user_id", UUID.class)),
                rs.getObject("conversation_id", UUID.class),
                rs.getObject("generation_id", UUID.class),
                rs.getString("model_id"),
                UsageSource.valueOf(rs.getString("source")),
                rs.getInt("prompt_tokens"),
                rs.getInt("completion_tokens"),
                rs.getInt("total_tokens"),
                rs.getBoolean("estimated"),
                rs.getLong("latency_ms"),
                UsageOutcome.valueOf(rs.getString("outcome")),
                rs.getObject("recorded_at", OffsetDateTime.class).toInstant()
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
