package io.github.jvpgjava.creatingj.conversation.adapter.out.streaming;

import io.github.jvpgjava.creatingj.conversation.application.port.out.ActiveGenerationRegistryPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public final class InMemoryActiveGenerationRegistryAdapter implements ActiveGenerationRegistryPort {

    private final Map<UUID, AtomicBoolean> activeGenerations = new ConcurrentHashMap<>();

    @Override
    public void register(UUID generationId) {
        activeGenerations.put(generationId, new AtomicBoolean(false));
    }

    @Override
    public boolean requestCancel(UUID generationId) {
        AtomicBoolean flag = activeGenerations.get(generationId);
        if (flag == null) {
            return false;
        }
        flag.set(true);
        return true;
    }

    @Override
    public boolean isCancelled(UUID generationId) {
        AtomicBoolean flag = activeGenerations.get(generationId);
        return flag != null && flag.get();
    }

    @Override
    public void unregister(UUID generationId) {
        activeGenerations.remove(generationId);
    }
}
