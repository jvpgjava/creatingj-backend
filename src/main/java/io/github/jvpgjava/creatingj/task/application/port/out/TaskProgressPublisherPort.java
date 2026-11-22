package io.github.jvpgjava.creatingj.task.application.port.out;

import io.github.jvpgjava.creatingj.task.domain.TaskEvent;

public interface TaskProgressPublisherPort {

    void publish(TaskEvent event);
}
