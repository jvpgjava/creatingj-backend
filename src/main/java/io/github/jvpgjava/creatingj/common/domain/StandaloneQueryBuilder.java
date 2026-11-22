package io.github.jvpgjava.creatingj.common.domain;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class StandaloneQueryBuilder {

    public static final int FOLLOWUP_MAX_WORDS = 6;
    public static final int MAX_STANDALONE_CHARS = 500;
    public static final int MAX_PREVIOUS_CONTEXT_CHARS = 280;

    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");

    private static final List<String> ANAPHORIC_PREFIXES = List.of(
            "e no ",
            "e na ",
            "e nos ",
            "e nas ",
            "e em ",
            "e para ",
            "e com ",
            "e sobre ",
            "e se ",
            "e quando ",
            "como assim",
            "mostre um exemplo",
            "me de um exemplo",
            "pode detalhar",
            "explique melhor",
            "qual a diferenca",
            "e o outro",
            "e a outra"
    );

    private static final Set<String> SHORT_AFFIRMATIONS = Set.of(
            "sim", "isso", "exato", "ok", "pode", "claro", "por favor", "mostre", "exemplo", "codigo"
    );

    private StandaloneQueryBuilder() {
    }

    public static String buildStandaloneQuery(String currentQuery, List<String> previousUserMessages) {
        if (currentQuery == null || currentQuery.isBlank()) {
            return "";
        }
        String cleanedCurrent = collapseWhitespace(currentQuery);
        if (previousUserMessages == null || previousUserMessages.isEmpty()) {
            return truncate(cleanedCurrent, MAX_STANDALONE_CHARS);
        }
        if (!needsContextExpansion(cleanedCurrent)) {
            return truncate(cleanedCurrent, MAX_STANDALONE_CHARS);
        }
        String previousTopic = findLastSubstantiveMessage(previousUserMessages, cleanedCurrent);
        if (previousTopic.isEmpty()) {
            return truncate(cleanedCurrent, MAX_STANDALONE_CHARS);
        }
        String combined = truncate(previousTopic, MAX_PREVIOUS_CONTEXT_CHARS) + " — " + cleanedCurrent;
        return truncate(combined, MAX_STANDALONE_CHARS);
    }

    public static boolean needsContextExpansion(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalized = PromptSecurityGuard.normalize(query);
        if (normalized.isEmpty()) {
            return false;
        }
        if (SHORT_AFFIRMATIONS.contains(normalized)) {
            return true;
        }
        for (String prefix : ANAPHORIC_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        String[] words = WORD_SPLIT.split(normalized);
        return words.length <= FOLLOWUP_MAX_WORDS;
    }

    private static String findLastSubstantiveMessage(List<String> previousUserMessages, String current) {
        String normalizedCurrent = PromptSecurityGuard.normalize(current);
        for (int i = previousUserMessages.size() - 1; i >= 0; i--) {
            String candidate = previousUserMessages.get(i);
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String cleaned = collapseWhitespace(candidate);
            String normCandidate = PromptSecurityGuard.normalize(cleaned);
            if (!normCandidate.equals(normalizedCurrent) && normCandidate.length() >= 8) {
                return cleaned;
            }
        }
        return "";
    }

    private static String collapseWhitespace(String text) {
        return WORD_SPLIT.matcher(text.trim()).replaceAll(" ");
    }

    private static String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars).trim();
    }
}
