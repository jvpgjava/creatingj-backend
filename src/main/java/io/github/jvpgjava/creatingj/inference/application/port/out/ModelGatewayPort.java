package io.github.jvpgjava.creatingj.inference.application.port.out;

import io.github.jvpgjava.creatingj.inference.domain.GenerationChunk;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public interface ModelGatewayPort {

    ModelCompletionResult complete(GenerationRequest request);

    ModelCompletionResult stream(
            GenerationRequest request,
            Consumer<GenerationChunk> onChunk,
            BooleanSupplier cancelled
    );

    List<String> listAvailableModels();
}
