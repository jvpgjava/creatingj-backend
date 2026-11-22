package io.github.jvpgjava.creatingj.task.domain;

public enum TaskEventType {
    TASK_QUEUED,
    TASK_LEASED,
    PROGRESS,
    APPROVAL_REQUESTED,
    APPROVAL_DECIDED,
    TASK_COMPLETED,
    TASK_FAILED,
    TASK_CANCELLED
}
