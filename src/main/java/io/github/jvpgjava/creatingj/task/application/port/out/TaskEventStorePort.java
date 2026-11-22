package io.github.jvpgjava.creatingj.task.application.port.out;

import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskId;

import java.util.List;
import java.util.Optional;

public interface TaskEventStorePort {

    Optional<TaskEvent> findByTaskAndEventKey(TaskId taskId, String eventKey);

    TaskEvent save(TaskEvent event);

    List<TaskEvent> findByTaskOrderByCreatedAsc(TaskId taskId);
}
