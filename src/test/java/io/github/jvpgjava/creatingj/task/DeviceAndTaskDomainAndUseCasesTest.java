package io.github.jvpgjava.creatingj.task;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.in.CompleteDevicePairingUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.CreatePairingChallengeUseCase;
import io.github.jvpgjava.creatingj.device.application.port.in.RevokeDeviceUseCase;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.PairingChallengeStorePort;
import io.github.jvpgjava.creatingj.device.application.usecase.CompleteDevicePairingInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.CreatePairingChallengeInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.ListUserDevicesInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.RecordDeviceHeartbeatInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.ResolveAuthenticatedDeviceInteractor;
import io.github.jvpgjava.creatingj.device.application.usecase.RevokeDeviceInteractor;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceCredential;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.device.domain.DevicePlatform;
import io.github.jvpgjava.creatingj.device.domain.DeviceStatus;
import io.github.jvpgjava.creatingj.device.domain.PairingChallenge;
import io.github.jvpgjava.creatingj.identity.adapter.out.security.SecureOpaqueTokenAdapter;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import io.github.jvpgjava.creatingj.task.application.port.in.ExecuteRunnerTaskProtocolUseCase;
import io.github.jvpgjava.creatingj.task.application.port.in.ManageUserRemoteTasksUseCase;
import io.github.jvpgjava.creatingj.task.application.port.out.RemoteTaskStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskEventStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.ToolApprovalStorePort;
import io.github.jvpgjava.creatingj.task.application.usecase.ExecuteRunnerTaskProtocolInteractor;
import io.github.jvpgjava.creatingj.task.application.usecase.ManageUserRemoteTasksInteractor;
import io.github.jvpgjava.creatingj.task.domain.RemoteApprovalStatus;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.TaskStatus;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceAndTaskDomainAndUseCasesTest {

    @Test
    void shouldHandleDevicePairingHeartbeatRevocationAndRemoteTaskLifecycle() {
        AtomicReference<Instant> nowRef = new AtomicReference<>(Instant.parse("2026-11-22T16:00:00Z"));
        ClockPort clock = nowRef::get;
        SecureOpaqueTokenAdapter tokens = new SecureOpaqueTokenAdapter();
        List<AuditEvent> audits = new ArrayList<>();
        List<TaskEvent> publishedEvents = new ArrayList<>();

        InMemoryDeviceStore deviceStore = new InMemoryDeviceStore();
        InMemoryChallengeStore challengeStore = new InMemoryChallengeStore();
        InMemoryCredentialStore credentialStore = new InMemoryCredentialStore();
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        InMemoryEventStore eventStore = new InMemoryEventStore();
        InMemoryApprovalStore approvalStore = new InMemoryApprovalStore();

        UserId userA = UserId.newId();
        UserId userB = UserId.newId();
        Project projA = Project.create(ProjectId.random(), userA, "App", "app", "", nowRef.get());
        ProjectStorePort projectStore = fakeProjectStore(Map.of(projA.id(), projA));

        var createChallenge = new CreatePairingChallengeInteractor(challengeStore, tokens, audits::add, clock);
        var completePairing = new CompleteDevicePairingInteractor(
                challengeStore, deviceStore, credentialStore, tokens, audits::add, clock);
        var listDevices = new ListUserDevicesInteractor(deviceStore, clock);
        var revokeDevice = new RevokeDeviceInteractor(deviceStore, credentialStore, audits::add, clock);
        var heartbeat = new RecordDeviceHeartbeatInteractor(deviceStore, clock);
        var resolveDevice = new ResolveAuthenticatedDeviceInteractor(credentialStore, deviceStore, tokens);

        var userTasks = new ManageUserRemoteTasksInteractor(
                deviceStore, projectStore, taskStore, eventStore, approvalStore,
                publishedEvents::add, audits::add, clock
        );
        var runnerTasks = new ExecuteRunnerTaskProtocolInteractor(
                taskStore, eventStore, approvalStore, publishedEvents::add, audits::add, clock
        );

        var challenge = createChallenge.execute(
                new CreatePairingChallengeUseCase.Command(userA, "Workstation Linux", DevicePlatform.LINUX));
        var paired = completePairing.execute(new CompleteDevicePairingUseCase.Command(challenge.pairingCode()));
        DeviceId deviceA = paired.device().id();

        assertThat(resolveDevice.execute(paired.deviceToken())).isPresent();
        assertThat(resolveDevice.execute(" ")).isEmpty();
        assertThatThrownBy(() -> completePairing.execute(
                new CompleteDevicePairingUseCase.Command(challenge.pairingCode())))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(() -> completePairing.execute(new CompleteDevicePairingUseCase.Command(" ")))
                .isInstanceOf(DomainException.class);

        nowRef.set(nowRef.get().plusSeconds(120));
        assertThat(listDevices.execute(userA).get(0).online()).isFalse();

        var offlineCreated = userTasks.create(new ManageUserRemoteTasksUseCase.CreateCommand(
                userA, deviceA, projA.id(), "idem-1", "Run tests", "Execute mvn test"
        ));
        assertThat(offlineCreated.deviceOnline()).isFalse();
        assertThat(offlineCreated.deduplicated()).isFalse();
        assertThat(offlineCreated.task().status()).isEqualTo(TaskStatus.QUEUED);

        var dedupCreated = userTasks.create(new ManageUserRemoteTasksUseCase.CreateCommand(
                userA, deviceA, projA.id(), "idem-1", "Run tests", "Execute mvn test"
        ));
        assertThat(dedupCreated.deduplicated()).isTrue();
        assertThat(dedupCreated.task().id()).isEqualTo(offlineCreated.task().id());

        heartbeat.execute(deviceA);
        assertThat(listDevices.execute(userA).get(0).online()).isTrue();

        var leased = runnerTasks.leaseNext(deviceA).orElseThrow();
        assertThat(leased.status()).isEqualTo(TaskStatus.LEASED);
        assertThat(runnerTasks.leaseNext(deviceA)).isEmpty();

        var ev1 = runnerTasks.reportEvent(new ExecuteRunnerTaskProtocolUseCase.ReportEventCommand(
                deviceA, leased.id(), "step-1", "Starting build"));
        assertThat(ev1.deduplicated()).isFalse();
        var ev1Dup = runnerTasks.reportEvent(new ExecuteRunnerTaskProtocolUseCase.ReportEventCommand(
                deviceA, leased.id(), "step-1", "Starting build"));
        assertThat(ev1Dup.deduplicated()).isTrue();

        assertThatThrownBy(() -> runnerTasks.requestApproval(
                new ExecuteRunnerTaskProtocolUseCase.RequestApprovalCommand(
                        deviceA, leased.id(), "RUN_COMMAND", null, "curl https://evil.example", null, 120)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);

        ToolApproval expiredApproval = runnerTasks.requestApproval(
                new ExecuteRunnerTaskProtocolUseCase.RequestApprovalCommand(
                        deviceA, leased.id(), "WRITE_FILE", "src/App.java", null, "+line", 30));
        nowRef.set(nowRef.get().plusSeconds(45));
        assertThatThrownBy(() -> userTasks.decideApproval(new ManageUserRemoteTasksUseCase.DecisionCommand(
                userA, leased.id(), expiredApproval.id(), ToolDecisionAction.APPROVE)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(approvalStore.findById(expiredApproval.id()).orElseThrow().status())
                .isEqualTo(RemoteApprovalStatus.EXPIRED);

        ToolApproval activeApproval = runnerTasks.requestApproval(
                new ExecuteRunnerTaskProtocolUseCase.RequestApprovalCommand(
                        deviceA, leased.id(), "RUN_COMMAND", null, "mvn test", null, 120));
        assertThatThrownBy(() -> userTasks.decideApproval(new ManageUserRemoteTasksUseCase.DecisionCommand(
                userB, leased.id(), activeApproval.id(), ToolDecisionAction.APPROVE)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);

        var decided = userTasks.decideApproval(new ManageUserRemoteTasksUseCase.DecisionCommand(
                userA, leased.id(), activeApproval.id(), ToolDecisionAction.APPROVE));
        assertThat(decided.approval().status()).isEqualTo(RemoteApprovalStatus.APPROVED);
        assertThat(decided.task().status()).isEqualTo(TaskStatus.RUNNING);

        var finished = runnerTasks.completeTask(new ExecuteRunnerTaskProtocolUseCase.CompleteTaskCommand(
                deviceA, leased.id(), true, "All tests green", null));
        assertThat(finished.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(userTasks.list(userA, 0)).hasSize(1);
        assertThat(userTasks.getDetail(userA, leased.id()).events()).isNotEmpty();

        var secondTask = userTasks.create(new ManageUserRemoteTasksUseCase.CreateCommand(
                userA, deviceA, null, null, "Second task", "Do work")).task();
        assertThat(userTasks.cancel(userA, secondTask.id()).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(userTasks.cancel(userA, secondTask.id()).status()).isEqualTo(TaskStatus.CANCELLED);

        assertThatThrownBy(() -> revokeDevice.execute(new RevokeDeviceUseCase.Command(userB, deviceA)))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        Device revoked = revokeDevice.execute(new RevokeDeviceUseCase.Command(userA, deviceA));
        assertThat(revoked.status()).isEqualTo(DeviceStatus.REVOKED);
        assertThat(resolveDevice.execute(paired.deviceToken())).isEmpty();
        assertThatThrownBy(() -> heartbeat.execute(deviceA))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(() -> userTasks.create(new ManageUserRemoteTasksUseCase.CreateCommand(
                userA, deviceA, null, null, "Blocked", "Should fail")))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_STATE);

        assertThat(DeviceId.parse(deviceA.toString())).isEqualTo(deviceA);
        assertThat(TaskId.parse(leased.id().toString())).isEqualTo(leased.id());
        assertThat(ToolApprovalId.parse(activeApproval.id().toString())).isEqualTo(activeApproval.id());
        assertThatThrownBy(() -> DeviceId.parse("bad")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> TaskId.parse("bad")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ToolApprovalId.parse("bad")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new DeviceId(null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new TaskId(null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new ToolApprovalId(null)).isInstanceOf(DomainException.class);
    }

    private static ProjectStorePort fakeProjectStore(Map<ProjectId, Project> projects) {
        return new ProjectStorePort() {
            @Override
            public Project save(Project project) {
                return project;
            }

            @Override
            public Optional<Project> findById(ProjectId projectId) {
                return Optional.ofNullable(projects.get(projectId));
            }

            @Override
            public Optional<Project> findByOwnerAndSlug(UserId ownerUserId, String slug) {
                return Optional.empty();
            }

            @Override
            public List<Project> findByOwner(UserId ownerUserId, int offset, int limit) {
                return List.of();
            }

            @Override
            public long countByOwner(UserId ownerUserId) {
                return 0;
            }

            @Override
            public void deleteById(ProjectId projectId) {
            }
        };
    }

    private static final class InMemoryDeviceStore implements DeviceStorePort {

        private final Map<DeviceId, Device> store = new ConcurrentHashMap<>();

        @Override
        public Device save(Device device) {
            store.put(device.id(), device);
            return device;
        }

        @Override
        public Optional<Device> findById(DeviceId deviceId) {
            return Optional.ofNullable(store.get(deviceId));
        }

        @Override
        public List<Device> findByOwner(UserId ownerUserId) {
            return store.values().stream().filter(d -> d.isOwnedBy(ownerUserId)).toList();
        }
    }

    private static final class InMemoryChallengeStore implements PairingChallengeStorePort {

        private final Map<String, PairingChallenge> store = new ConcurrentHashMap<>();

        @Override
        public PairingChallenge save(PairingChallenge challenge) {
            store.put(challenge.codeHash(), challenge);
            return challenge;
        }

        @Override
        public Optional<PairingChallenge> findByCodeHash(String codeHash) {
            return Optional.ofNullable(store.get(codeHash));
        }
    }

    private static final class InMemoryCredentialStore implements DeviceCredentialStorePort {

        private final Map<String, DeviceCredential> store = new ConcurrentHashMap<>();

        @Override
        public DeviceCredential save(DeviceCredential credential) {
            store.put(credential.tokenHash(), credential);
            return credential;
        }

        @Override
        public Optional<DeviceCredential> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(store.get(tokenHash));
        }

        @Override
        public void revokeAllByDeviceId(DeviceId deviceId, Instant revokedAt) {
            store.replaceAll((k, v) -> v.deviceId().equals(deviceId) ? v.revoke(revokedAt) : v);
        }
    }

    private static final class InMemoryTaskStore implements RemoteTaskStorePort {

        private final Map<TaskId, RemoteTask> store = new ConcurrentHashMap<>();

        @Override
        public RemoteTask save(RemoteTask task) {
            store.put(task.id(), task);
            return task;
        }

        @Override
        public Optional<RemoteTask> findById(TaskId taskId) {
            return Optional.ofNullable(store.get(taskId));
        }

        @Override
        public Optional<RemoteTask> findByIdempotency(UserId ownerUserId, DeviceId deviceId, String idempotencyKey) {
            return store.values().stream()
                    .filter(t -> t.isOwnedBy(ownerUserId)
                            && t.isAssignedTo(deviceId)
                            && idempotencyKey.equals(t.idempotencyKey()))
                    .findFirst();
        }

        @Override
        public List<RemoteTask> findRecentByOwner(UserId ownerUserId, int limit) {
            return store.values().stream().filter(t -> t.isOwnedBy(ownerUserId)).limit(limit).toList();
        }

        @Override
        public Optional<RemoteTask> findNextLeasableForDevice(DeviceId deviceId, Instant now) {
            return store.values().stream()
                    .filter(t -> t.isAssignedTo(deviceId) && t.isLeasableAt(now))
                    .sorted(Comparator.comparing(RemoteTask::createdAt))
                    .findFirst();
        }
    }

    private static final class InMemoryEventStore implements TaskEventStorePort {

        private final List<TaskEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public Optional<TaskEvent> findByTaskAndEventKey(TaskId taskId, String eventKey) {
            return events.stream()
                    .filter(e -> e.taskId().equals(taskId) && e.eventKey().equals(eventKey))
                    .findFirst();
        }

        @Override
        public TaskEvent save(TaskEvent event) {
            if (findByTaskAndEventKey(event.taskId(), event.eventKey()).isEmpty()) {
                events.add(event);
            }
            return event;
        }

        @Override
        public List<TaskEvent> findByTaskOrderByCreatedAsc(TaskId taskId) {
            return events.stream().filter(e -> e.taskId().equals(taskId)).toList();
        }
    }

    private static final class InMemoryApprovalStore implements ToolApprovalStorePort {

        private final Map<ToolApprovalId, ToolApproval> store = new ConcurrentHashMap<>();

        @Override
        public ToolApproval save(ToolApproval approval) {
            store.put(approval.id(), approval);
            return approval;
        }

        @Override
        public Optional<ToolApproval> findById(ToolApprovalId approvalId) {
            return Optional.ofNullable(store.get(approvalId));
        }

        @Override
        public List<ToolApproval> findByTaskOrderByCreatedAsc(TaskId taskId) {
            return store.values().stream().filter(a -> a.taskId().equals(taskId)).toList();
        }
    }
}
