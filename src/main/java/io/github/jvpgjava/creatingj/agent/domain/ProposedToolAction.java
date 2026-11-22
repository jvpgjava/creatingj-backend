package io.github.jvpgjava.creatingj.agent.domain;

public record ProposedToolAction(
        String rawToolName,
        String targetPath,
        String commandText,
        String rationale
) {
}
