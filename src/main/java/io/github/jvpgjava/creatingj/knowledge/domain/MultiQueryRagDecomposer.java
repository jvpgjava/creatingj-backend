package io.github.jvpgjava.creatingj.knowledge.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class MultiQueryRagDecomposer {

    public static final int MAX_SUBQUERIES = 3;
    public static final int MIN_CLAUSE_LENGTH = 12;

    private static final Pattern CLAUSE_SPLITTER = Pattern.compile(
            "(?i)(?:\\s*;\\s*|\\s+e\\s+tambem\\s+|\\s+alem\\s+de\\s+|\\s*,\\s+e\\s+|\\r?\\n+)"
    );

    private MultiQueryRagDecomposer() {
    }

    public static List<String> decompose(String rawGoalOrQuery) {
        if (rawGoalOrQuery == null || rawGoalOrQuery.isBlank()) {
            return List.of();
        }
        String primary = rawGoalOrQuery.trim();
        String[] parts = CLAUSE_SPLITTER.split(primary);
        if (parts.length <= 1) {
            return List.of(primary);
        }

        Set<String> queries = new LinkedHashSet<>();
        queries.add(primary);
        for (String part : parts) {
            if (queries.size() >= MAX_SUBQUERIES) {
                break;
            }
            String trimmed = part.trim();
            if (trimmed.length() >= MIN_CLAUSE_LENGTH && !trimmed.equalsIgnoreCase(primary)) {
                queries.add(trimmed);
            }
        }
        return List.copyOf(queries);
    }

    public static List<KnowledgeSearchMatch> fuseMultiQueryResults(
            List<List<KnowledgeSearchMatch>> perQueryResults,
            int topK
    ) {
        if (perQueryResults == null || perQueryResults.isEmpty()) {
            return List.of();
        }
        if (perQueryResults.size() == 1) {
            List<KnowledgeSearchMatch> single = perQueryResults.get(0);
            return single.size() <= topK ? List.copyOf(single) : List.copyOf(single.subList(0, topK));
        }

        Map<String, KnowledgeSearchMatch> bestMatchByKey = new LinkedHashMap<>();
        Map<String, Double> rrfByKey = new HashMap<>();

        for (List<KnowledgeSearchMatch> ranking : perQueryResults) {
            if (ranking == null) {
                continue;
            }
            for (int pos = 0; pos < ranking.size(); pos++) {
                KnowledgeSearchMatch match = ranking.get(pos);
                String key = chunkKey(match);
                bestMatchByKey.merge(key, match,
                        (existing, candidate) -> candidate.score() > existing.score() ? candidate : existing);
                rrfByKey.merge(key, 1.0 / (HybridRagRanker.RRF_K + pos + 1), Double::sum);
            }
        }

        List<KnowledgeSearchMatch> merged = new ArrayList<>(bestMatchByKey.values());
        merged.sort(Comparator.comparingDouble((KnowledgeSearchMatch m) -> rrfByKey.getOrDefault(chunkKey(m), 0.0))
                .reversed()
                .thenComparing(Comparator.comparingDouble(KnowledgeSearchMatch::score).reversed()));

        int limit = Math.max(1, topK);
        return merged.size() <= limit ? List.copyOf(merged) : List.copyOf(merged.subList(0, limit));
    }

    public static List<KnowledgeSearchMatch> expandCutCodeNeighbors(
            List<KnowledgeSearchMatch> selectedMatches,
            List<KnowledgeSearchMatch> candidatePool,
            int maxResults
    ) {
        if (selectedMatches == null || selectedMatches.isEmpty()
                || candidatePool == null || candidatePool.isEmpty()) {
            return selectedMatches == null ? List.of() : selectedMatches;
        }

        Map<String, KnowledgeSearchMatch> poolByKey = new HashMap<>();
        for (KnowledgeSearchMatch candidate : candidatePool) {
            poolByKey.putIfAbsent(chunkKey(candidate), candidate);
        }

        Set<String> included = new LinkedHashSet<>();
        for (KnowledgeSearchMatch m : selectedMatches) {
            included.add(chunkKey(m));
        }

        List<KnowledgeSearchMatch> result = new ArrayList<>(selectedMatches.size() + 2);
        for (KnowledgeSearchMatch match : selectedMatches) {
            result.add(match);
            if (result.size() < maxResults && looksLikeCutCodeOrSection(match.excerpt())) {
                String nextKey = match.sourceId().value() + ":" + (match.chunkIndex() + 1);
                KnowledgeSearchMatch nextNeighbor = poolByKey.get(nextKey);
                if (nextNeighbor != null && included.add(nextKey)) {
                    result.add(nextNeighbor);
                }
            }
            if (result.size() >= maxResults) {
                break;
            }
        }
        return List.copyOf(result);
    }

    public static boolean looksLikeCutCodeOrSection(String excerpt) {
        if (excerpt == null || excerpt.isBlank()) {
            return false;
        }
        int fences = 0;
        int idx = 0;
        while ((idx = excerpt.indexOf("```", idx)) >= 0) {
            fences++;
            idx += 3;
        }
        if (fences % 2 != 0) {
            return true;
        }
        int openBraces = 0;
        int closeBraces = 0;
        for (int i = 0; i < excerpt.length(); i++) {
            char c = excerpt.charAt(i);
            if (c == '{') {
                openBraces++;
            } else if (c == '}') {
                closeBraces++;
            }
        }
        return openBraces > closeBraces;
    }

    private static String chunkKey(KnowledgeSearchMatch match) {
        return match.sourceId().value() + ":" + match.chunkIndex();
    }
}
