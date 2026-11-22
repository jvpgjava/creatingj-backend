package io.github.jvpgjava.creatingj.project;

import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.application.port.in.CreateProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.DeleteProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.GetProjectUseCase;
import io.github.jvpgjava.creatingj.project.application.port.in.ListAuthorizedProjectsUseCase;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.application.usecase.CreateProjectInteractor;
import io.github.jvpgjava.creatingj.project.application.usecase.DeleteProjectInteractor;
import io.github.jvpgjava.creatingj.project.application.usecase.GetProjectInteractor;
import io.github.jvpgjava.creatingj.project.application.usecase.ListAuthorizedProjectsInteractor;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectDomainAndUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");

    @Test
    void shouldValidateProjectDomainAndDeriveSlug() {
        UserId owner = UserId.newId();
        Project created = Project.create(ProjectId.random(), owner, "CreatingJ Backend!", null, "  Core API  ", NOW);
        assertThat(created.slug()).isEqualTo("creatingj-backend");
        assertThat(created.description()).isEqualTo("Core API");
        assertThat(created.isOwnedBy(owner)).isTrue();
        assertThat(created.isOwnedBy(UserId.newId())).isFalse();
        assertThat(ProjectId.parse(created.id().toString())).isEqualTo(created.id());

        assertThatThrownBy(() -> ProjectId.parse("not-a-uuid"))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> ProjectId.parse(" "))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new ProjectId(null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Project.create(ProjectId.random(), owner, "Test", "BAD SLUG!", "", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Project.create(ProjectId.random(), owner, "!!!", null, "", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Project.create(ProjectId.random(), owner, " ", null, "", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Project.create(ProjectId.random(), owner, "Valid", "valid", "x".repeat(501), NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Project(null, owner, "Valid", "valid", "", NOW, NOW))
                .isInstanceOf(DomainException.class);
        assertThat(Project.deriveSlug("a".repeat(140))).hasSize(120);
    }

    @Test
    void shouldEnforceOwnershipConflictAndCascadeCleanupInUseCases() {
        InMemoryProjectStore store = new InMemoryProjectStore();
        List<AuditEvent> audits = new ArrayList<>();
        ClockPort clock = () -> NOW;
        AtomicBoolean cleaned = new AtomicBoolean(false);

        CreateProjectInteractor create = new CreateProjectInteractor(store, audits::add, clock);
        ListAuthorizedProjectsInteractor list = new ListAuthorizedProjectsInteractor(store);
        GetProjectInteractor get = new GetProjectInteractor(store);
        DeleteProjectInteractor delete = new DeleteProjectInteractor(
                store,
                (pid, uid) -> cleaned.set(true),
                audits::add,
                clock
        );

        UserId userA = UserId.newId();
        UserId userB = UserId.newId();

        Project projA = create.execute(new CreateProjectUseCase.Command(userA, "Alpha App", null, "Docs"));
        assertThat(projA.slug()).isEqualTo("alpha-app");
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).action()).isEqualTo(AuditAction.PROJECT_CREATED);

        assertThatThrownBy(() -> create.execute(new CreateProjectUseCase.Command(userA, "Alpha Two", "alpha-app", "")))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT);

        Project projB = create.execute(new CreateProjectUseCase.Command(userB, "Alpha App", "alpha-app", null));
        assertThat(projB.ownerUserId()).isEqualTo(userB);

        var pageA = list.execute(new ListAuthorizedProjectsUseCase.Query(userA, -1, 0));
        assertThat(pageA.items()).containsExactly(projA);
        assertThat(pageA.page()).isZero();
        assertThat(pageA.size()).isEqualTo(20);
        assertThat(pageA.totalElements()).isEqualTo(1);

        assertThat(get.execute(new GetProjectUseCase.Query(userA, projA.id()))).isEqualTo(projA);
        assertThatThrownBy(() -> get.execute(new GetProjectUseCase.Query(userB, projA.id())))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> get.execute(new GetProjectUseCase.Query(userA, ProjectId.random())))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);

        assertThatThrownBy(() -> delete.execute(new DeleteProjectUseCase.Command(userB, projA.id())))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);

        delete.execute(new DeleteProjectUseCase.Command(userA, projA.id()));
        assertThat(cleaned.get()).isTrue();
        assertThat(store.findById(projA.id())).isEmpty();
        assertThat(audits.get(audits.size() - 1).action()).isEqualTo(AuditAction.PROJECT_DELETED);

        assertThatThrownBy(() -> create.execute(new CreateProjectUseCase.Command(null, "X", null, null)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(() -> list.execute(new ListAuthorizedProjectsUseCase.Query(null, 0, 10)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(() -> get.execute(new GetProjectUseCase.Query(userA, null)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> delete.execute(new DeleteProjectUseCase.Command(userA, null)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private static final class InMemoryProjectStore implements ProjectStorePort {

        private final Map<ProjectId, Project> store = new ConcurrentHashMap<>();

        @Override
        public Project save(Project project) {
            store.put(project.id(), project);
            return project;
        }

        @Override
        public Optional<Project> findById(ProjectId projectId) {
            return Optional.ofNullable(store.get(projectId));
        }

        @Override
        public Optional<Project> findByOwnerAndSlug(UserId ownerUserId, String slug) {
            return store.values().stream()
                    .filter(p -> p.ownerUserId().equals(ownerUserId) && p.slug().equals(slug))
                    .findFirst();
        }

        @Override
        public List<Project> findByOwner(UserId ownerUserId, int offset, int limit) {
            return store.values().stream()
                    .filter(p -> p.ownerUserId().equals(ownerUserId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countByOwner(UserId ownerUserId) {
            return store.values().stream().filter(p -> p.ownerUserId().equals(ownerUserId)).count();
        }

        @Override
        public void deleteById(ProjectId projectId) {
            store.remove(projectId);
        }
    }
}
