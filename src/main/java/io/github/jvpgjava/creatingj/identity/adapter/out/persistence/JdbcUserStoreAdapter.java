package io.github.jvpgjava.creatingj.identity.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "creatingj.persistence.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcUserStoreAdapter implements UserStorePort {

    private final JdbcClient jdbcClient;

    public JdbcUserStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public User save(User user) {
        jdbcClient.sql("""
                        INSERT INTO users
                            (id, email, display_name, password_hash, status,
                             created_by, created_at, updated_at, suspended_at)
                        VALUES
                            (:id, :email, :displayName, :passwordHash, :status,
                             :createdBy, :createdAt, :updatedAt, :suspendedAt)
                        ON CONFLICT (id) DO UPDATE SET
                            display_name = EXCLUDED.display_name,
                            password_hash = EXCLUDED.password_hash,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at,
                            suspended_at = EXCLUDED.suspended_at
                        """)
                .param("id", user.id().value())
                .param("email", user.email().value())
                .param("displayName", user.displayName())
                .param("passwordHash", user.passwordHash())
                .param("status", user.status().name())
                .param("createdBy", user.createdBy() == null ? null : user.createdBy().value())
                .param("createdAt", toOffset(user.createdAt()))
                .param("updatedAt", toOffset(user.updatedAt()))
                .param("suspendedAt", toOffset(user.suspendedAt()))
                .update();

        jdbcClient.sql("DELETE FROM user_roles WHERE user_id = :userId")
                .param("userId", user.id().value())
                .update();

        for (Role role : user.roles()) {
            jdbcClient.sql("INSERT INTO user_roles (user_id, role) VALUES (:userId, :role)")
                    .param("userId", user.id().value())
                    .param("role", role.name())
                    .update();
        }
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        Optional<UserRow> row = jdbcClient.sql("""
                        SELECT id, email, display_name, password_hash, status,
                               created_by, created_at, updated_at, suspended_at
                        FROM users
                        WHERE id = :id
                        """)
                .param("id", id.value())
                .query(this::mapUserRow)
                .optional();
        return row.map(this::hydrateRoles);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        Optional<UserRow> row = jdbcClient.sql("""
                        SELECT id, email, display_name, password_hash, status,
                               created_by, created_at, updated_at, suspended_at
                        FROM users
                        WHERE email = :email
                        """)
                .param("email", email.value())
                .query(this::mapUserRow)
                .optional();
        return row.map(this::hydrateRoles);
    }

    @Override
    public boolean existsByEmail(Email email) {
        Integer count = jdbcClient.sql("SELECT count(*) FROM users WHERE email = :email")
                .param("email", email.value())
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public List<User> findPage(int page, int size, AccountStatus status, UserSortField sortBy, boolean ascending) {
        String orderColumn = switch (sortBy) {
            case EMAIL -> "email";
            case DISPLAY_NAME -> "display_name";
            case STATUS -> "status";
            case CREATED_AT -> "created_at";
        };
        String direction = ascending ? "ASC" : "DESC";
        String whereClause = status == null ? "" : "WHERE status = :status ";
        String sql = "SELECT id, email, display_name, password_hash, status, "
                + "created_by, created_at, updated_at, suspended_at "
                + "FROM users "
                + whereClause
                + "ORDER BY " + orderColumn + " " + direction + ", id ASC "
                + "LIMIT :limit OFFSET :offset";

        var spec = jdbcClient.sql(sql)
                .param("limit", size)
                .param("offset", (long) page * size);
        if (status != null) {
            spec = spec.param("status", status.name());
        }

        return spec.query(this::mapUserRow)
                .list()
                .stream()
                .map(this::hydrateRoles)
                .toList();
    }

    @Override
    public long count(AccountStatus status) {
        if (status == null) {
            Long total = jdbcClient.sql("SELECT count(*) FROM users").query(Long.class).single();
            return total == null ? 0L : total;
        }
        Long total = jdbcClient.sql("SELECT count(*) FROM users WHERE status = :status")
                .param("status", status.name())
                .query(Long.class)
                .single();
        return total == null ? 0L : total;
    }

    private User hydrateRoles(UserRow row) {
        Set<Role> roles = jdbcClient.sql("SELECT role FROM user_roles WHERE user_id = :userId")
                .param("userId", row.id())
                .query(String.class)
                .list()
                .stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        return new User(
                new UserId(row.id()),
                Email.of(row.email()),
                row.displayName(),
                row.passwordHash(),
                AccountStatus.valueOf(row.status()),
                roles,
                row.createdBy() == null ? null : new UserId(row.createdBy()),
                row.createdAt(),
                row.updatedAt(),
                row.suspendedAt()
        );
    }

    private UserRow mapUserRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserRow(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("password_hash"),
                rs.getString("status"),
                rs.getObject("created_by", UUID.class),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)),
                toInstant(rs.getObject("suspended_at", OffsetDateTime.class))
        );
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }

    private record UserRow(
            UUID id,
            String email,
            String displayName,
            String passwordHash,
            String status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant suspendedAt
    ) {
    }
}
