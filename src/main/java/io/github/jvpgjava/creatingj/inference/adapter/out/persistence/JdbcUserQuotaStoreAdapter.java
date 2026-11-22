package io.github.jvpgjava.creatingj.inference.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcUserQuotaStoreAdapter implements UserQuotaStorePort {

    private final JdbcClient jdbcClient;

    public JdbcUserQuotaStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UserQuota> findByUserId(UserId userId) {
        return jdbcClient.sql("""
                        SELECT user_id, daily_token_limit, daily_request_limit,
                               max_concurrent_generations, updated_by, updated_at
                        FROM user_quotas
                        WHERE user_id = :userId
                        """)
                .param("userId", userId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public UserQuota save(UserQuota quota) {
        jdbcClient.sql("""
                        INSERT INTO user_quotas
                            (user_id, daily_token_limit, daily_request_limit,
                             max_concurrent_generations, updated_by, updated_at)
                        VALUES
                            (:userId, :dailyTokenLimit, :dailyRequestLimit,
                             :maxConcurrentGenerations, :updatedBy, :updatedAt)
                        ON CONFLICT (user_id) DO UPDATE SET
                            daily_token_limit = EXCLUDED.daily_token_limit,
                            daily_request_limit = EXCLUDED.daily_request_limit,
                            max_concurrent_generations = EXCLUDED.max_concurrent_generations,
                            updated_by = EXCLUDED.updated_by,
                            updated_at = EXCLUDED.updated_at
                        """)
                .param("userId", quota.userId().value())
                .param("dailyTokenLimit", quota.dailyTokenLimit())
                .param("dailyRequestLimit", quota.dailyRequestLimit())
                .param("maxConcurrentGenerations", quota.maxConcurrentGenerations())
                .param("updatedBy", quota.updatedBy() == null ? null : quota.updatedBy().value())
                .param("updatedAt", OffsetDateTime.ofInstant(quota.updatedAt(), ZoneOffset.UTC))
                .update();
        return quota;
    }

    private UserQuota mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID updatedBy = rs.getObject("updated_by", UUID.class);
        return new UserQuota(
                new UserId(rs.getObject("user_id", UUID.class)),
                rs.getLong("daily_token_limit"),
                rs.getInt("daily_request_limit"),
                rs.getInt("max_concurrent_generations"),
                updatedBy == null ? null : new UserId(updatedBy),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
