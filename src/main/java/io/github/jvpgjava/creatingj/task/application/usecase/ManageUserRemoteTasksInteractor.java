package io.github.jvpgjava.creatingj.task.application.usecase;

import io.github.jvpgjava.creatingj.agent.domain.ToolDecisionAction;
import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.task.application.port.in.ManageUserRemoteTasksUseCase;
import io.github.jvpgjava.creatingj.task.application.port.out.RemoteTaskStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskEventStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskProgressPublisherPort;
import io.github.jvpgjava.creatingj.task.application.port.out.ToolApprovalStorePort;
import io.github.jvpgjava.creatingj.task.domain.RemoteApprovalStatus;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskEventType;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.TaskStatus;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ManageUserRemoteTasksInteractor implements ManageUserRemoteTasksUseCase {

    private final DeviceStorePort deviceStore;
    private final ProjectStorePort projectStore;
    private final RemoteTaskStorePort taskStore;
    private final TaskEventStorePort eventStore;
    private final ToolApprovalStorePort approvalStore;
    private final TaskProgressPublisherPort progressPublisher;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public ManageUserRemoteTasksInteractor(
            DeviceStorePort deviceStore,
            ProjectStorePort projectStore,
            RemoteTaskStorePort taskStore,
            TaskEventStorePort eventStore,
            ToolApprovalStorePort approvalStore,
            TaskProgressPublisherPort progressPublisher,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.deviceStore = deviceStore;
        this.projectStore = projectStore;
        this.taskStore = taskStore;
        this.eventStore = eventStore;
        this.approvalStore = approvalStore;
        this.progressPublisher = progressPublisher;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public CreateResult create(CreateCommand command) {
        Device device = deviceStore.findById(command.deviceId())
                .filter(d -> d.isOwnedBy(command.actorUserId()))
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Dispositivo nao encontrado."));
        if (!device.isActive()) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Dispositivo revogado nao aceita novas tarefas.");
        }
        if (command.projectId() != null) {
            Project project = projectStore.findById(command.projectId())
                    .filter(p -> p.isOwnedBy(command.actorUserId()))
                    .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado."));
            if (!project.isOwnedBy(command.actorUserId())) {
                throw new DomainException(ErrorCode.NOT_FOUND, "Projeto nao encontrado.");
            }
        }
        Instant now = clock.now();
        boolean online = device.isOnline(now);
        String idempotencyKey = command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                ? null
                : command.idempotencyKey().trim();
        if (idempotencyKey != null) {
            Optional<RemoteTask> existing = taskStore.findByIdempotency(
                    command.actorUserId(), command.deviceId(), idempotencyKey);
            if (existing.isPresent()) {
                return new CreateResult(existing.get(), online, true);
            }
        }

        RemoteTask task = taskStore.save(new RemoteTask(
                TaskId.random(),
                command.actorUserId(),
                command.projectId(),
                command.deviceId(),
                idempotencyKey,
                command.title(),
                command.instruction(),
                TaskStatus.QUEUED,
                null,
                null,
                null,
                now,
                now,
                null
        ));
        TaskEvent queuedEvent = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                task.id(),
                "queued-" + task.id(),
                TaskEventType.TASK_QUEUED,
                online ? "Tarefa enfileirada para o dispositivo." : "Tarefa enfileirada; dispositivo offline.",
                now
        ));
        progressPublisher.publish(queuedEvent);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                AuditAction.REMOTE_TASK_CREATED,
                "SUCCESS",
                "taskId=" + task.id() + " deviceId=" + device.id() + " online=" + online,
                now
        ));
        return new CreateResult(task, online, false);
    }

    @Override
    public List<RemoteTask> list(UserId actorUserId, int limit) {
        int resolvedLimit = limit <= 0 ? 20 : Math.min(limit, 50);
        return taskStore.findRecentByOwner(actorUserId, resolvedLimit);
    }

    @Override
    public TaskDetail getDetail(UserId actorUserId, TaskId taskId) {
        RemoteTask task = requireOwnedTask(actorUserId, taskId);
        List<TaskEvent> events = eventStore.findByTaskOrderByCreatedAsc(task.id());
        List<ToolApproval> approvals = approvalStore.findByTaskOrderByCreatedAsc(task.id());
        return new TaskDetail(task, events, approvals);
    }

    @Override
    public RemoteTask cancel(UserId actorUserId, TaskId taskId) {
        RemoteTask task = requireOwnedTask(actorUserId, taskId);
        if (task.status().isTerminal()) {
            return task;
        }
        Instant now = clock.now();
        RemoteTask cancelled = taskStore.save(task.cancel(now));
        for (ToolApproval approval : approvalStore.findByTaskOrderByCreatedAsc(task.id())) {
            if (approval.status() == RemoteApprovalStatus.PENDING) {
                approvalStore.save(approval.expireOrRejectIfPending(RemoteApprovalStatus.REJECTED, now));
            }
        }
        TaskEvent event = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                task.id(),
                "cancelled-" + task.id(),
                TaskEventType.TASK_CANCELLED,
                "Tarefa cancelada pelo proprietario.",
                now
        ));
        progressPublisher.publish(event);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                actorUserId.value(),
                actorUserId.value(),
                AuditAction.REMOTE_TASK_CANCELLED,
                "SUCCESS",
                "taskId=" + task.id(),
                now
        ));
        return cancelled;
    }

    @Override
    public DecisionResult decideApproval(DecisionCommand command) {
        RemoteTask task = requireOwnedTask(command.actorUserId(), command.taskId());
        if (task.status().isTerminal()) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Tarefa finalizada nao aceita aprovacoes.");
        }
        ToolApproval approval = approvalStore.findById(command.approvalId())
                .filter(a -> a.belongsTo(task.id(), command.actorUserId()))
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Aprovacao nao encontrada."));
        Instant now = clock.now();
        if (approval.status() == RemoteApprovalStatus.PENDING && approval.isExpiredAt(now)) {
            approvalStore.save(approval.expireOrRejectIfPending(RemoteApprovalStatus.EXPIRED, now));
            throw new DomainException(ErrorCode.INVALID_STATE, "Aprovacao pontual expirada.");
        }
        ToolApproval decided = approvalStore.save(approval.decide(command.action(), now));
        RemoteTask updatedTask = command.action() == ToolDecisionAction.APPROVE
                ? taskStore.save(task.markRunning(now))
                : taskStore.save(task.complete(false, "Execucao rejeitada pelo usuario.", "APPROVAL_REJECTED", now));

        TaskEvent event = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                task.id(),
                "approval-" + decided.id() + "-" + decided.status().name(),
                TaskEventType.APPROVAL_DECIDED,
                "Aprovacao " + decided.status().name() + " para " + decided.toolName().name(),
                now
        ));
        progressPublisher.publish(event);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                command.actorUserId().value(),
                command.actorUserId().value(),
                command.action() == ToolDecisionAction.APPROVE
                        ? AuditAction.TOOL_APPROVAL_APPROVED
                        : AuditAction.TOOL_APPROVAL_REJECTED,
                "SUCCESS",
                "taskId=" + task.id() + " approvalId=" + decided.id() + " tool=" + decided.toolName(),
                now
        ));
        return new DecisionResult(updatedTask, decided);
    }

    private RemoteTask requireOwnedTask(UserId actorUserId, TaskId taskId) {
        return taskStore.findById(taskId)
                .filter(t -> t.isOwnedBy(actorUserId))
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Tarefa nao encontrada."));
    }
}
