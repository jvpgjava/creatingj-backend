package io.github.jvpgjava.creatingj.task.domain;

public enum TaskStatus {
    QUEUED,
    LEASED,
    RUNNING,
    AWAITING_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == EXPIRED;
    }
}
