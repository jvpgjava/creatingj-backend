package io.github.jvpgjava.creatingj.inference.domain;

public record GenerationChunk(
        String deltaText,
        String finishReason,
        TokenUsage usage
) {
}
