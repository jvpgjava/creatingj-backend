package io.github.jvpgjava.creatingj.common.domain;

import java.util.List;

public final class FollowUpSuggestionEngine {

    public static final int MAX_FOLLOWUPS = 3;

    private FollowUpSuggestionEngine() {
    }

    public static List<String> suggestFollowUps(
            String userQuestion,
            String assistantAnswer,
            boolean guardrailBlocked
    ) {
        if (guardrailBlocked) {
            return List.of();
        }
        String combined = PromptSecurityGuard.normalize(
                (userQuestion == null ? "" : userQuestion) + " " + (assistantAnswer == null ? "" : assistantAnswer)
        );
        if (combined.isEmpty()) {
            return List.of();
        }
        if (containsAny(combined, "jwt", "pkce", "csrf", "cookie", "argon2", "sessao", "login", "autentic")) {
            return List.of(
                    "Como testar esse fluxo de autenticacao com MockMvc?",
                    "Quais cabecalhos de seguranca protegem essa sessao em producao?",
                    "Como funciona a revogacao em caso de reuso de token?"
            );
        }
        if (containsAny(combined, "pgvector", "embedding", "onnx", "rag", "chunk", "tsvector", "flyway", "postgres")) {
            return List.of(
                    "Como funciona a busca hibrida com pgvector e tsvector?",
                    "Como reindexar os documentos ao trocar a dimensao no .env?",
                    "Quais indices HNSW e GIN estao ativos no PostgreSQL?"
            );
        }
        if (containsAny(combined, "agente", "tool", "runner", "device", "lease", "approval", "tarefa")) {
            return List.of(
                    "Quais comandos passam pela allowlist da politica deterministica?",
                    "Como funciona a aprovacao pontual com TTL no runner remoto?",
                    "Como acompanhar os eventos da tarefa via streaming SSE?"
            );
        }
        if (containsAny(combined, "teste", "junit", "jacoco", "archunit", "checkstyle", "maven", "jenkins")) {
            return List.of(
                    "Como executar a suite completa de integracao com PostgreSQL?",
                    "Quais fronteiras arquiteturais o ArchUnit valida no build?",
                    "Como verificar a cobertura minima exigida pelo JaCoCo?"
            );
        }
        return List.of(
                "Pode mostrar um exemplo pratico de implementacao em Java 25?",
                "Como posso cobrir esse comportamento com testes automatizados?",
                "Quais cuidados de seguranca devo validar nesse cenario?"
        );
    }

    private static boolean containsAny(String normalizedText, String... keywords) {
        for (String keyword : keywords) {
            if (normalizedText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
