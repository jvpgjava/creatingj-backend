package io.github.jvpgjava.creatingj.conversation.adapter.out.persistence;

import io.github.jvpgjava.creatingj.conversation.application.port.out.MessageStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.MessageRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcMessageStoreAdapter implements MessageStorePort {

    private final JdbcClient jdbcClient;

    public JdbcMessageStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        jdbcClient.sql("""
                        INSERT INTO messages
                            (id, conversation_id, role, content, sequence_num, created_at)
                        VALUES
                            (:id, :conversationId, :role, :content, :sequenceNum, :createdAt)
                        """)
                .param("id", message.id())
                .param("conversationId", message.conversationId().value())
                .param("role", message.role().name())
                .param("content", message.content())
                .param("sequenceNum", message.sequenceNum())
                .param("createdAt", OffsetDateTime.ofInstant(message.createdAt(), ZoneOffset.UTC))
                .update();
        return message;
    }

    @Override
    public List<ChatMessage> findByConversationOrderBySequenceAsc(ConversationId conversationId) {
        return jdbcClient.sql("""
                        SELECT id, conversation_id, role, content, sequence_num, created_at
                        FROM messages
                        WHERE conversation_id = :conversationId
                        ORDER BY sequence_num ASC
                        """)
                .param("conversationId", conversationId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public int nextSequenceNumber(ConversationId conversationId) {
        Integer maxSeq = jdbcClient.sql(
                        "SELECT COALESCE(MAX(sequence_num), 0) FROM messages WHERE conversation_id = :conversationId")
                .param("conversationId", conversationId.value())
                .query(Integer.class)
                .single();
        return (maxSeq == null ? 0 : maxSeq) + 1;
    }

    private ChatMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ChatMessage(
                rs.getObject("id", UUID.class),
                new ConversationId(rs.getObject("conversation_id", UUID.class)),
                MessageRole.valueOf(rs.getString("role")),
                rs.getString("content"),
                rs.getInt("sequence_num"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
