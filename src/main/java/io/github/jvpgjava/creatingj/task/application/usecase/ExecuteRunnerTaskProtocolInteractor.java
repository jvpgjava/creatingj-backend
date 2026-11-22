package io.github.jvpgjava.creatingj.task.application.usecase;

import io.github.jvpgjava.creatingj.agent.domain.ProposedToolAction;
import io.github.jvpgjava.creatingj.agent.domain.ToolExecutionPolicyEngine;
import io.github.jvpgjava.creatingj.agent.domain.ToolPolicyDecision;
import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditAction;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.task.application.port.in.ExecuteRunnerTaskProtocolUseCase;
import io.github.jvpgjava.creatingj.task.application.port.out.RemoteTaskStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskEventStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskProgressPublisherPort;
import io.github.jvpgjava.creatingj.task.application.port.out.ToolApprovalStorePort;
import io.github.jvpgjava.creatingj.task.domain.RemoteApprovalStatus;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskEventType;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class ExecuteRunnerTaskProtocolInteractor implements ExecuteRunnerTaskProtocolUseCase {

    private static final int MIN_APPROVAL_TTL_SECONDS = 30;
    private static final int MAX_APPROVAL_TTL_SECONDS = 600;
    private static final int MAX_DIFF_PREVIEW_CHARS = 8000;

    private final RemoteTaskStorePort taskStore;
    private final TaskEventStorePort eventStore;
    private final ToolApprovalStorePort approvalStore;
    private final TaskProgressPublisherPort progressPublisher;
    private final AuditPort auditPort;
    private final ClockPort clock;

    public ExecuteRunnerTaskProtocolInteractor(
            RemoteTaskStorePort taskStore,
            TaskEventStorePort eventStore,
            ToolApprovalStorePort approvalStore,
            TaskProgressPublisherPort progressPublisher,
            AuditPort auditPort,
            ClockPort clock
    ) {
        this.taskStore = taskStore;
        this.eventStore = eventStore;
        this.approvalStore = approvalStore;
        this.progressPublisher = progressPublisher;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public Optional<RemoteTask> leaseNext(DeviceId deviceId) {
        Instant now = clock.now();
        Optional<RemoteTask> candidate = taskStore.findNextLeasableForDevice(deviceId, now);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        RemoteTask leased = taskStore.save(candidate.get().lease(now));
        TaskEvent event = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                leased.id(),
                "leased-" + now.toEpochMilli() + "-" + UUID.randomUUID(),
                TaskEventType.TASK_LEASED,
                "Tarefa adquirida pelo dispositivo autorizado.",
                now
        ));
        progressPublisher.publish(event);
        return Optional.of(leased);
    }

    @Override
    public EventResult reportEvent(ReportEventCommand command) {
        RemoteTask task = requireAssignedActiveTask(command.deviceId(), command.taskId());
        String key = command.eventKey() == null ? "" : command.eventKey().trim();
        Optional<TaskEvent> existing = eventStore.findByTaskAndEventKey(task.id(), key);
        if (existing.isPresent()) {
            return new EventResult(existing.get(), true);
        }
        Instant now = clock.now();
        taskStore.save(task.markRunning(now));
        TaskEvent saved = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                task.id(),
                key,
                TaskEventType.PROGRESS,
                command.message(),
                now
        ));
        progressPublisher.publish(saved);
        return new EventResult(saved, false);
    }

    @Override
    public ToolApproval requestApproval(RequestApprovalCommand command) {
        RemoteTask task = requireAssignedActiveTask(command.deviceId(), command.taskId());
        String diffPreview = command.diffPreview();
        if (diffPreview != null && diffPreview.length() > MAX_DIFF_PREVIEW_CHARS) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Preview de diff excede o limite permitido.");
        }
        ToolExecutionPolicyEngine.Evaluation evaluation = ToolExecutionPolicyEngine.evaluateWithDiff(
                new ProposedToolAction(
                        command.toolName(),
                        command.targetPath(),
                        command.commandText(),
                        "Runner request"
                ),
                diffPreview
        );
        Instant now = clock.now();
        if (evaluation.policyDecision() == ToolPolicyDecision.DENIED_BY_POLICY) {
            auditPort.record(AuditEvent.of(
                    UUID.randomUUID(),
                    task.ownerUserId().value(),
                    task.ownerUserId().value(),
                    AuditAction.AGENT_TOOL_DENIED,
                    "DENIED",
                    "taskId=" + task.id() + " deviceId=" + command.deviceId() + " tool=" + evaluation.toolName(),
                    now
            ));
            throw new DomainException(ErrorCode.VALIDATION_ERROR, evaluation.policyReason());
        }
        int ttl = Math.max(MIN_APPROVAL_TTL_SECONDS, Math.min(command.ttlSeconds(), MAX_APPROVAL_TTL_SECONDS));
        ToolApproval saved = approvalStore.save(new ToolApproval(
                ToolApprovalId.random(),
                task.id(),
                task.ownerUserId(),
                command.deviceId(),
                evaluation.toolName(),
                evaluation.normalizedTargetPath(),
                evaluation.sanitizedCommand(),
                diffPreview,
                RemoteApprovalStatus.PENDING,
                now.plusSeconds(ttl),
                null,
                now
        ));
        taskStore.save(task.markAwaitingApproval(now));
        TaskEvent event = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                task.id(),
                "approval-req-" + saved.id(),
                TaskEventType.APPROVAL_REQUESTED,
                "Aprovacao pontual solicitada para " + saved.toolName().name(),
                now
        ));
        progressPublisher.publish(event);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                task.ownerUserId().value(),
                task.ownerUserId().value(),
                AuditAction.TOOL_APPROVAL_REQUESTED,
                "PENDING",
                "taskId=" + task.id() + " approvalId=" + saved.id() + " tool=" + saved.toolName(),
                now
        ));
        return saved;
    }

    @Override
    public RemoteTask completeTask(CompleteTaskCommand command) {
        RemoteTask task = requireAssignedActiveTask(command.deviceId(), command.taskId());
        Instant now = clock.now();
        RemoteTask finished = taskStore.save(
                task.complete(command.succeeded(), command.resultSummary(), command.errorCode(), now)
        );
        TaskEvent event = eventStore.save(new TaskEvent(
                UUID.randomUUID(),
                task.id(),
                "finished-" + task.id(),
                command.succeeded() ? TaskEventType.TASK_COMPLETED : TaskEventType.TASK_FAILED,
                command.succeeded() ? "Tarefa concluida pelo runner." : "Tarefa falhou no runner.",
                now
        ));
        progressPublisher.publish(event);
        auditPort.record(AuditEvent.of(
                UUID.randomUUID(),
                task.ownerUserId().value(),
                task.ownerUserId().value(),
                AuditAction.REMOTE_TASK_COMPLETED,
                command.succeeded() ? "SUCCESS" : "FAILED",
                "taskId=" + task.id() + " deviceId=" + command.deviceId(),
                now
        ));
        return finished;
    }

    private RemoteTask requireAssignedActiveTask(DeviceId deviceId, TaskId taskId) {
        RemoteTask task = taskStore.findById(taskId)
                .filter(t -> t.isAssignedTo(deviceId))
                .orElseThrow(() -> new DomainException(
                        ErrorCode.NOT_FOUND,
                        "Tarefa nao encontrada para o dispositivo."
                ));
        if (task.status().isTerminal()) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Tarefa ja finalizada ou cancelada.");
        }
        return task;
    }
}
