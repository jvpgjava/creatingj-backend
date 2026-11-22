package io.github.jvpgjava.creatingj.project.adapter.out.persistence;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
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
public class JdbcProjectStoreAdapter implements ProjectStorePort {

    private final JdbcClient jdbcClient;

    public JdbcProjectStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Project save(Project project) {
        jdbcClient.sql("""
                INSERT INTO projects (id, owner_user_id, name, slug, description, created_at, updated_at)
                VALUES (:id, :ownerUserId, :name, :slug, :description, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    slug = EXCLUDED.slug,
                    description = EXCLUDED.description,
                    updated_at = EXCLUDED.updated_at
                """)
                .param("id", project.id().value())
                .param("ownerUserId", project.ownerUserId().value())
                .param("name", project.name())
                .param("slug", project.slug())
                .param("description", project.description())
                .param("createdAt", OffsetDateTime.ofInstant(project.createdAt(), ZoneOffset.UTC))
                .param("updatedAt", OffsetDateTime.ofInstant(project.updatedAt(), ZoneOffset.UTC))
                .update();
        return project;
    }

    @Override
    public Optional<Project> findById(ProjectId projectId) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, name, slug, description, created_at, updated_at
                FROM projects
                WHERE id = :id
                """)
                .param("id", projectId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<Project> findByOwnerAndSlug(UserId ownerUserId, String slug) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, name, slug, description, created_at, updated_at
                FROM projects
                WHERE owner_user_id = :ownerUserId AND slug = :slug
                """)
                .param("ownerUserId", ownerUserId.value())
                .param("slug", slug)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Project> findByOwner(UserId ownerUserId, int offset, int limit) {
        return jdbcClient.sql("""
                SELECT id, owner_user_id, name, slug, description, created_at, updated_at
                FROM projects
                WHERE owner_user_id = :ownerUserId
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """)
                .param("ownerUserId", ownerUserId.value())
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapRow)
                .list();
    }

    @Override
    public long countByOwner(UserId ownerUserId) {
        Long count = jdbcClient.sql("SELECT COUNT(*) FROM projects WHERE owner_user_id = :ownerUserId")
                .param("ownerUserId", ownerUserId.value())
                .query(Long.class)
                .single();
        return count == null ? 0L : count;
    }

    @Override
    public void deleteById(ProjectId projectId) {
        jdbcClient.sql("DELETE FROM projects WHERE id = :id")
                .param("id", projectId.value())
                .update();
    }

    private Project mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Project(
                new ProjectId(rs.getObject("id", UUID.class)),
                new UserId(rs.getObject("owner_user_id", UUID.class)),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
