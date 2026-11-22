package io.github.jvpgjava.creatingj.task.adapter.in.web.streaming;

import io.github.jvpgjava.creatingj.task.adapter.in.web.mapper.v1.TaskHttpMapper;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskProgressPublisherPort;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SseTaskProgressHub implements TaskProgressPublisherPort {

    private final Map<TaskId, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final TaskHttpMapper mapper;

    public SseTaskProgressHub(TaskHttpMapper mapper) {
        this.mapper = mapper;
    }

    public SseEmitter subscribe(TaskId taskId, List<TaskEvent> initialSnapshot, boolean terminal) {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            for (TaskEvent event : initialSnapshot) {
                emitter.send(SseEmitter.event()
                        .id(event.eventKey())
                        .name(event.eventType().name().toLowerCase())
                        .data(mapper.toEvent(event)));
            }
            if (terminal) {
                emitter.complete();
                return emitter;
            }
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return emitter;
        }
        List<SseEmitter> list = subscribers.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable cleanup = () -> list.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        return emitter;
    }

    @Override
    public void publish(TaskEvent event) {
        List<SseEmitter> list = subscribers.get(event.taskId());
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventKey())
                        .name(event.eventType().name().toLowerCase())
                        .data(mapper.toEvent(event)));
            } catch (IOException ex) {
                list.remove(emitter);
                emitter.complete();
            }
        }
    }
}
