package io.github.jvpgjava.creatingj.conversation.adapter.in.web.streaming;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.mapper.v1.ConversationHttpMapper;
import io.github.jvpgjava.creatingj.conversation.application.port.in.StreamMessageGenerationUseCase;
import io.github.jvpgjava.creatingj.conversation.application.port.in.StreamMessageGenerationUseCase.StreamMessageCommand;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public final class SseGenerationBridge {

    private static final long SSE_TIMEOUT_MS = Duration.ofMinutes(3).toMillis();

    private final StreamMessageGenerationUseCase streamUseCase;
    private final ConversationHttpMapper mapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SseGenerationBridge(
            StreamMessageGenerationUseCase streamUseCase,
            ConversationHttpMapper mapper
    ) {
        this.streamUseCase = streamUseCase;
        this.mapper = mapper;
    }

    public SseEmitter openStream(StreamMessageCommand command) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean disconnected = new AtomicBoolean(false);
        emitter.onCompletion(() -> disconnected.set(true));
        emitter.onTimeout(() -> disconnected.set(true));
        emitter.onError(err -> disconnected.set(true));

        executor.submit(() -> {
            try {
                streamUseCase.execute(command, event -> {
                    if (disconnected.get()) {
                        return;
                    }
                    try {
                        emitter.send(SseEmitter.event()
                                .id(event.eventId())
                                .name(event.type())
                                .data(mapper.toSsePayload(event), MediaType.APPLICATION_JSON));
                    } catch (IOException ex) {
                        disconnected.set(true);
                    }
                });
                if (!disconnected.get()) {
                    emitter.complete();
                }
            } catch (DomainException ex) {
                emitter.completeWithError(ex);
            } catch (RuntimeException ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
