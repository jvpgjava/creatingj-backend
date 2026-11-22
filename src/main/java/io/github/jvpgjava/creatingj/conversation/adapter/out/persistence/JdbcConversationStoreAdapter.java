package io.github.jvpgjava.creatingj.conversation.adapter.out.persistence;

import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
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
public class JdbcConversationStoreAdapter implements ConversationStorePort {

    private final JdbcClient jdbcClient;

    public JdbcConversationStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Conversation save(Conversation conversation) {
        jdbcClient.sql("""
                        INSERT INTO conversations
                            (id, owner_user_id, title, model_id, created_at, updated_at, archived_at)
                        VALUES
                            (:id, :ownerUserId, :title, :modelId, :createdAt, :updatedAt, :archivedAt)
                        ON CONFLICT (id) DO UPDATE SET
                            title = EXCLUDED.title,
                            updated_at = EXCLUDED.updated_at,
                            archived_at = EXCLUDED.archived_at
                        """)
                .param("id", conversation.id().value())
                .param("ownerUserId", conversation.ownerUserId().value())
                .param("title", conversation.title())
                .param("modelId", conversation.modelId())
                .param("createdAt", toOffset(conversation.createdAt()))
                .param("updatedAt", toOffset(conversation.updatedAt()))
                .param("archivedAt", toOffset(conversation.archivedAt()))
                .update();
        return conversation;
    }

    @Override
    public Optional<Conversation> findByIdAndOwner(ConversationId id, UserId ownerUserId) {
        return jdbcClient.sql("""
                        SELECT id, owner_user_id, title, model_id, created_at, updated_at, archived_at
                        FROM conversations
                        WHERE id = :id AND owner_user_id = :ownerUserId
                        """)
                .param("id", id.value())
                .param("ownerUserId", ownerUserId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Conversation> findPageByOwner(UserId ownerUserId, int page, int size) {
        return jdbcClient.sql("""
                        SELECT id, owner_user_id, title, model_id, created_at, updated_at, archived_at
                        FROM conversations
                        WHERE owner_user_id = :ownerUserId
                        ORDER BY updated_at DESC, id ASC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("ownerUserId", ownerUserId.value())
                .param("limit", size)
                .param("offset", (long) page * size)
                .query(this::mapRow)
                .list();
    }

    @Override
    public long countByOwner(UserId ownerUserId) {
        Long total = jdbcClient.sql("SELECT count(*) FROM conversations WHERE owner_user_id = :ownerUserId")
                .param("ownerUserId", ownerUserId.value())
                .query(Long.class)
                .single();
        return total == null ? 0L : total;
    }

    private Conversation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Conversation(
                new ConversationId(rs.getObject("id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("title"),
                rs.getString("model_id"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)),
                toInstant(rs.getObject("archived_at", OffsetDateTime.class))
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
