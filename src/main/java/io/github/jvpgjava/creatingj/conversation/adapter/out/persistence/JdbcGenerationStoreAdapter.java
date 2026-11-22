package io.github.jvpgjava.creatingj.conversation.adapter.out.persistence;

import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.conversation.domain.GenerationStatus;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
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
public class JdbcGenerationStoreAdapter implements GenerationStorePort {

    private static final String BASE_SELECT = """
            SELECT id, conversation_id, owner_user_id, user_message_id, assistant_message_id,
                   model_id, status, prompt_tokens, completion_tokens, total_tokens,
                   error_code, created_at, completed_at
            FROM generations
            """;

    private final JdbcClient jdbcClient;

    public JdbcGenerationStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Generation save(Generation generation) {
        jdbcClient.sql("""
                        INSERT INTO generations
                            (id, conversation_id, owner_user_id, user_message_id, assistant_message_id,
                             model_id, status, prompt_tokens, completion_tokens, total_tokens,
                             error_code, created_at, completed_at)
                        VALUES
                            (:id, :conversationId, :ownerUserId, :userMessageId, :assistantMessageId,
                             :modelId, :status, :promptTokens, :completionTokens, :totalTokens,
                             :errorCode, :createdAt, :completedAt)
                        ON CONFLICT (id) DO UPDATE SET
                            assistant_message_id = EXCLUDED.assistant_message_id,
                            status = EXCLUDED.status,
                            prompt_tokens = EXCLUDED.prompt_tokens,
                            completion_tokens = EXCLUDED.completion_tokens,
                            total_tokens = EXCLUDED.total_tokens,
                            error_code = EXCLUDED.error_code,
                            completed_at = EXCLUDED.completed_at
                        """)
                .param("id", generation.id())
                .param("conversationId", generation.conversationId().value())
                .param("ownerUserId", generation.ownerUserId().value())
                .param("userMessageId", generation.userMessageId())
                .param("assistantMessageId", generation.assistantMessageId())
                .param("modelId", generation.modelId())
                .param("status", generation.status().name())
                .param("promptTokens", generation.promptTokens())
                .param("completionTokens", generation.completionTokens())
                .param("totalTokens", generation.totalTokens())
                .param("errorCode", generation.errorCode())
                .param("createdAt", toOffset(generation.createdAt()))
                .param("completedAt", toOffset(generation.completedAt()))
                .update();
        return generation;
    }

    @Override
    public Optional<Generation> findByIdAndConversationAndOwner(
            UUID generationId,
            ConversationId conversationId,
            UserId ownerUserId
    ) {
        return jdbcClient.sql(BASE_SELECT + """
                         WHERE id = :id
                           AND conversation_id = :conversationId
                           AND owner_user_id = :ownerUserId
                        """)
                .param("id", generationId)
                .param("conversationId", conversationId.value())
                .param("ownerUserId", ownerUserId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Generation> findByConversationOrderByCreatedAtDesc(ConversationId conversationId) {
        return jdbcClient.sql(BASE_SELECT + """
                         WHERE conversation_id = :conversationId
                         ORDER BY created_at DESC
                        """)
                .param("conversationId", conversationId.value())
                .query(this::mapRow)
                .list();
    }

    private Generation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Generation(
                rs.getObject("id", UUID.class),
                new ConversationId(rs.getObject("conversation_id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getObject("user_message_id", UUID.class),
                rs.getObject("assistant_message_id", UUID.class),
                rs.getString("model_id"),
                GenerationStatus.valueOf(rs.getString("status")),
                rs.getObject("prompt_tokens", Integer.class),
                rs.getObject("completion_tokens", Integer.class),
                rs.getObject("total_tokens", Integer.class),
                rs.getString("error_code"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
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
