package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.PromptSecurityGuard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HybridRagRanker {

    public static final int RRF_K = 60;
    public static final int SMALL_DOCUMENT_MAX_CHUNKS = 4;
    public static final int MAX_RELAXED_TERMS = 12;
    public static final double LEXICAL_RRF_BONUS_WEIGHT = 8.0;

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}_]{2,}");

    private static final Set<String> PORTUGUESE_STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas", "um", "uma",
            "uns", "umas", "para", "por", "com", "sem", "sob", "sobre", "que", "qual",
            "quais", "como", "onde", "quando", "porque", "isso", "esse", "essa",
            "este", "esta", "seu", "sua", "seus", "suas", "meu", "minha", "foi", "sao",
            "ser", "tem", "ter", "mais", "mas", "ou", "se", "ao", "aos", "as", "os"
    );

    private HybridRagRanker() {
    }

    public static List<String> extractSearchTerms(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }
        String normalized = PromptSecurityGuard.normalize(rawQuery);
        Matcher matcher = WORD_PATTERN.matcher(normalized);
        Set<String> terms = new LinkedHashSet<>();
        while (matcher.find() && terms.size() < MAX_RELAXED_TERMS) {
            String term = matcher.group();
            if (!PORTUGUESE_STOPWORDS.contains(term)) {
                terms.add(term);
            }
        }
        return List.copyOf(terms);
    }

    public static String buildRelaxedWebsearchQuery(String rawQuery) {
        List<String> terms = extractSearchTerms(rawQuery);
        if (terms.isEmpty()) {
            return "";
        }
        return String.join(" OR ", terms);
    }

    public static List<KnowledgeSearchMatch> fuseVectorAndLexical(
            List<KnowledgeSearchMatch> vectorMatches,
            List<KnowledgeSearchMatch> lexicalMatches,
            int topK,
            double minScore
    ) {
        List<KnowledgeSearchMatch> safeVector = vectorMatches == null ? List.of() : vectorMatches;
        List<KnowledgeSearchMatch> safeLexical = lexicalMatches == null ? List.of() : lexicalMatches;
        if (safeVector.isEmpty() && safeLexical.isEmpty()) {
            return List.of();
        }

        Map<String, KnowledgeSearchMatch> byKey = new LinkedHashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Double> baseVectorScores = new HashMap<>();

        for (int i = 0; i < safeVector.size(); i++) {
            KnowledgeSearchMatch m = safeVector.get(i);
            String key = chunkKey(m);
            byKey.putIfAbsent(key, m);
            baseVectorScores.put(key, m.score());
            rrfScores.merge(key, 1.0 / (RRF_K + i + 1), Double::sum);
        }

        for (int i = 0; i < safeLexical.size(); i++) {
            KnowledgeSearchMatch m = safeLexical.get(i);
            String key = chunkKey(m);
            byKey.putIfAbsent(key, m);
            baseVectorScores.putIfAbsent(key, m.score());
            rrfScores.merge(key, 1.0 / (RRF_K + i + 1), Double::sum);
        }

        boolean hasBothSides = !safeVector.isEmpty() && !safeLexical.isEmpty();
        Set<String> lexicalKeys = new LinkedHashSet<>();
        for (KnowledgeSearchMatch m : safeLexical) {
            lexicalKeys.add(chunkKey(m));
        }

        List<KnowledgeSearchMatch> fused = new ArrayList<>(byKey.size());
        for (Map.Entry<String, KnowledgeSearchMatch> entry : byKey.entrySet()) {
            String key = entry.getKey();
            KnowledgeSearchMatch match = entry.getValue();
            double baseScore = baseVectorScores.getOrDefault(key, match.score());
            double rrf = rrfScores.getOrDefault(key, 0.0);
            double hybridScore = baseScore;
            if (hasBothSides && lexicalKeys.contains(key)) {
                double lexicalBonus = Math.min(0.15, rrf * LEXICAL_RRF_BONUS_WEIGHT);
                hybridScore = Math.min(1.0, baseScore + lexicalBonus);
            }
            if (hybridScore >= minScore) {
                fused.add(new KnowledgeSearchMatch(
                        match.sourceId(),
                        match.sourcePath(),
                        match.title(),
                        match.chunkIndex(),
                        match.excerpt(),
                        hybridScore,
                        match.embeddingVersion()
                ));
            }
        }

        fused.sort(Comparator.comparingDouble((KnowledgeSearchMatch m) -> rrfScores.getOrDefault(chunkKey(m), 0.0))
                .reversed()
                .thenComparing(Comparator.comparingDouble(KnowledgeSearchMatch::score).reversed())
                .thenComparingInt(KnowledgeSearchMatch::chunkIndex));

        int limit = Math.max(1, topK);
        return fused.size() <= limit ? List.copyOf(fused) : List.copyOf(fused.subList(0, limit));
    }

    public static List<KnowledgeSearchMatch> expandSmallDocumentSiblings(
            List<KnowledgeSearchMatch> selectedMatches,
            List<KnowledgeSearchMatch> candidatePool,
            Map<KnowledgeSourceId, Integer> sourceChunkCounts,
            int maxResults
    ) {
        if (selectedMatches == null || selectedMatches.isEmpty()
                || candidatePool == null || candidatePool.isEmpty()
                || sourceChunkCounts == null || sourceChunkCounts.isEmpty()) {
            return selectedMatches == null ? List.of() : selectedMatches;
        }
        Set<String> presentKeys = new LinkedHashSet<>();
        for (KnowledgeSearchMatch m : selectedMatches) {
            presentKeys.add(chunkKey(m));
        }

        Map<KnowledgeSourceId, List<KnowledgeSearchMatch>> siblingsBySource = new HashMap<>();
        for (KnowledgeSearchMatch candidate : candidatePool) {
            int totalChunks = sourceChunkCounts.getOrDefault(candidate.sourceId(), Integer.MAX_VALUE);
            if (totalChunks <= SMALL_DOCUMENT_MAX_CHUNKS && !presentKeys.contains(chunkKey(candidate))) {
                siblingsBySource.computeIfAbsent(candidate.sourceId(), k -> new ArrayList<>()).add(candidate);
            }
        }
        if (siblingsBySource.isEmpty()) {
            return selectedMatches;
        }
        for (List<KnowledgeSearchMatch> list : siblingsBySource.values()) {
            list.sort(Comparator.comparingInt(KnowledgeSearchMatch::chunkIndex));
        }

        List<KnowledgeSearchMatch> expanded = new ArrayList<>();
        Set<KnowledgeSourceId> expandedSources = new LinkedHashSet<>();
        for (KnowledgeSearchMatch match : selectedMatches) {
            expanded.add(match);
            if (expandedSources.add(match.sourceId())) {
                List<KnowledgeSearchMatch> extras = siblingsBySource.getOrDefault(match.sourceId(), List.of());
                for (KnowledgeSearchMatch sibling : extras) {
                    if (expanded.size() >= maxResults) {
                        break;
                    }
                    if (presentKeys.add(chunkKey(sibling))) {
                        expanded.add(sibling);
                    }
                }
            }
            if (expanded.size() >= maxResults) {
                break;
            }
        }
        return List.copyOf(expanded);
    }

    private static String chunkKey(KnowledgeSearchMatch match) {
        return match.sourceId().value() + ":" + match.chunkIndex();
    }
}
