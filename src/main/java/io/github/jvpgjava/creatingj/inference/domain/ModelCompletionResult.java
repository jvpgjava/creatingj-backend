package io.github.jvpgjava.creatingj.inference.domain;

public record ModelCompletionResult(
        String content,
        String modelId,
        String finishReason,
        TokenUsage usage
) {
}
