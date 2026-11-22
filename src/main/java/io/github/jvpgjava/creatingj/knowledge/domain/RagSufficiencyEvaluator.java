package io.github.jvpgjava.creatingj.knowledge.domain;

import io.github.jvpgjava.creatingj.common.domain.PromptSecurityGuard;

import java.util.List;

public final class RagSufficiencyEvaluator {

    public static final double SUFFICIENT_SCORE_THRESHOLD = 0.60;
    public static final double PARTIAL_SCORE_THRESHOLD = 0.32;

    public static final String SUFFICIENT_NOTE =
            "[Suficiencia do contexto RAG: SUFICIENTE. Baseie os fatos do projeto nos trechos acima. "
                    + "As referencias [T#] sao estritamente internas: nunca as cite na resposta final.]";

    public static final String PARTIAL_NOTE =
            "[Suficiencia do contexto RAG: PARCIAL. Os trechos acima cobrem apenas parte da consulta. "
                    + "Utilize apenas os fatos explicitamente confirmados nos trechos e nao invente detalhes "
                    + "ausentes do projeto. As referencias [T#] sao internas: nunca as cite.]";

    public static final String INSUFFICIENT_NOTE =
            "[Suficiencia do contexto RAG: INSUFICIENTE. Nenhum trecho relevante do projeto confirmou "
                    + "os fatos especificos solicitados. Nao invente caminhos, arquivos ou configuracoes do "
                    + "projeto; informe claramente o que precisa ser inspecionado no workspace.]";

    private RagSufficiencyEvaluator() {
    }

    public static RagSufficiencyLevel evaluate(String queryText, List<KnowledgeSearchMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return RagSufficiencyLevel.INSUFFICIENT;
        }
        double topScore = matches.stream()
                .mapToDouble(KnowledgeSearchMatch::score)
                .max()
                .orElse(0.0);

        List<String> queryTerms = HybridRagRanker.extractSearchTerms(queryText);
        double termCoverage = computeTermCoverage(queryTerms, matches);

        if (topScore >= SUFFICIENT_SCORE_THRESHOLD || (topScore >= PARTIAL_SCORE_THRESHOLD && termCoverage >= 0.65)) {
            return RagSufficiencyLevel.SUFFICIENT;
        }
        if (topScore >= PARTIAL_SCORE_THRESHOLD || termCoverage >= 0.30) {
            return RagSufficiencyLevel.PARTIAL;
        }
        return RagSufficiencyLevel.INSUFFICIENT;
    }

    public static String guidanceNote(RagSufficiencyLevel level) {
        if (level == null) {
            return INSUFFICIENT_NOTE;
        }
        return switch (level) {
            case SUFFICIENT -> SUFFICIENT_NOTE;
            case PARTIAL -> PARTIAL_NOTE;
            case INSUFFICIENT -> INSUFFICIENT_NOTE;
        };
    }

    private static double computeTermCoverage(List<String> queryTerms, List<KnowledgeSearchMatch> matches) {
        if (queryTerms.isEmpty()) {
            return 1.0;
        }
        StringBuilder combined = new StringBuilder();
        for (KnowledgeSearchMatch match : matches) {
            combined.append(' ').append(PromptSecurityGuard.normalize(match.excerpt()));
            combined.append(' ').append(PromptSecurityGuard.normalize(match.title()));
        }
        String haystack = combined.toString();
        int matched = 0;
        for (String term : queryTerms) {
            if (haystack.contains(term)) {
                matched++;
            }
        }
        return (double) matched / queryTerms.size();
    }

    public enum RagSufficiencyLevel {
        SUFFICIENT,
        PARTIAL,
        INSUFFICIENT
    }
}
