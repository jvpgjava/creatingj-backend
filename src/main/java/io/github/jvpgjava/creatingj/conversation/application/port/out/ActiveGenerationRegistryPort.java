package io.github.jvpgjava.creatingj.conversation.application.port.out;

import java.util.UUID;

public interface ActiveGenerationRegistryPort {

    void register(UUID generationId);

    boolean requestCancel(UUID generationId);

    boolean isCancelled(UUID generationId);

    void unregister(UUID generationId);
}
