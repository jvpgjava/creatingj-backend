package io.github.jvpgjava.creatingj.agent.domain;

import java.util.Locale;

public enum AgentToolName {
    READ_FILE,
    SEARCH_CODE,
    WRITE_FILE,
    RUN_COMMAND,
    UNKNOWN;

    public static AgentToolName fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "READ_FILE" -> READ_FILE;
            case "SEARCH_CODE" -> SEARCH_CODE;
            case "WRITE_FILE" -> WRITE_FILE;
            case "RUN_COMMAND" -> RUN_COMMAND;
            default -> UNKNOWN;
        };
    }
}
