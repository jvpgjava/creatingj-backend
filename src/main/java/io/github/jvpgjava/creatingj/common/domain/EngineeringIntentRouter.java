package io.github.jvpgjava.creatingj.common.domain;

import java.util.ArrayList;
import java.util.List;

public final class EngineeringIntentRouter {

    private static final String PLAYBOOK_DEBUG = """
            # Playbook Ativo: Diagnostico e Correcao de Bugs (DEBUG_AND_FIX)
            - Identifique primeiro a causa-raiz provavel a partir da mensagem de erro, stacktrace ou sintoma.
            - Proponha uma correcao cirurgica, preservando contratos existentes e tratando casos nulos/bordas.
            - Indique exatamente como reproduzir e validar a correcao com um teste automatizado na stack utilizada.
            """;

    private static final String PLAYBOOK_TESTING = """
            # Playbook Ativo: Engenharia de Testes e QA (TESTING_AND_QA)
            - Estruture testes deterministas cobrindo: caminho feliz, limites/bordas, entradas invalidas e isolamento.
            - Evite testes frageis dependentes de tempo real, ordem de execucao ou rede externa nao controlada.
            - Utilize os idioms nativos do framework de testes da linguagem (ex.: JUnit 5/AssertJ, Vitest/Jest, \
            Pytest, Cargo test, Go testing, xUnit).
            """;

    private static final String PLAYBOOK_SECURITY = """
            # Playbook Ativo: Revisao de Seguranca e Autenticacao (SECURITY_AND_AUTH)
            - Aplique defesa em profundidade (OWASP Top 10): validacao estrita de entrada, queries parametrizadas, \
            escapamento de saida, anti-SSRF, anti-IDOR e principio do menor privilegio.
            - Nunca exponha segredos, stacktraces ou detalhes internos em respostas HTTP; priorize contratos de erro \
            estruturados (como RFC 9457 ProblemDetail).
            """;

    private static final String PLAYBOOK_DATABASE = """
            # Playbook Ativo: Banco de Dados, Migrations e Busca (DATABASE_AND_DATA)
            - Escreva migrations idempotentes e retrocompativeis, com constraints explicitas e indices adequados \
            ao padrao de leitura/escrita.
            - Previna problemas de N+1, lock excessivo e scans sequenciais desnecessarios; valide tipos e transacoes.
            """;

    private static final String PLAYBOOK_ARCHITECTURE = """
            # Playbook Ativo: Arquitetura Limpa e Refatoracao (ARCHITECTURE_AND_REFACTOR)
            - Mantenha separacao clara de responsabilidades: regras de negocio/dominio desacopladas de frameworks, \
            transporte HTTP, UI e persistencia.
            - Priorize imutabilidade, tipagem forte, nomes expressivos, funcoes coesas e baixo acoplamento em \
            qualquer linguagem ou paradigma.
            """;

    private static final String PLAYBOOK_GENERAL = """
            # Playbook Ativo: Engenharia de Software Multi-Linguagem (GENERAL_ENGINEERING)
            - Adapte-se idiomaticamente a linguagem, runtime e framework solicitados pelo desenvolvedor.
            - Entregue codigo completo, compilavel/executavel, legivel, seguro por padrao e facil de testar.
            """;

    private EngineeringIntentRouter() {
    }

    public static RouteProfile route(String userMessage) {
        String norm = PromptSecurityGuard.normalize(userMessage);
        EngineeringIntent intent = classifyIntent(norm);
        List<String> detectedStacks = detectStacks(norm);
        return new RouteProfile(intent, detectedStacks, buildPlaybookSection(intent, detectedStacks));
    }

    public static EngineeringIntent classifyIntent(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return EngineeringIntent.GENERAL_ENGINEERING;
        }
        if (containsAny(normalizedText,
                "exception", "stacktrace", "traceback", "nullpointer", "panic", "segfault",
                "nao compila", "erro ", "bug", "falha", "corrigir", "fix ", "broken", "crash")) {
            return EngineeringIntent.DEBUG_AND_FIX;
        }
        if (containsAny(normalizedText,
                "junit", "pytest", "vitest", "jest", "playwright", "cypress", "rspec",
                "mockmvc", "mockito", "jacoco", "archunit", "teste unitario", "testes de integracao",
                "cobertura de teste", "unit test")) {
            return EngineeringIntent.TESTING_AND_QA;
        }
        if (containsAny(normalizedText,
                "owasp", "vulnerabil", "sqli", "sql injection", "xss", "csrf", "ssrf", "idor",
                "argon2", "bcrypt", "pkce", "oauth", "jwt", "cors", "criptograf", "autenticacao",
                "autorizacao", "seguranca")) {
            return EngineeringIntent.SECURITY_AND_AUTH;
        }
        if (containsAny(normalizedText,
                "postgresql", "postgres", "mysql", "sqlite", "mongodb", "redis", "flyway",
                "liquibase", "alembic", "prisma", "pgvector", "tsvector", "hnsw", "migration",
                "indice ", "tabela ", "query sql", "select ")) {
            return EngineeringIntent.DATABASE_AND_DATA;
        }
        if (containsAny(normalizedText,
                "arquitetura", "hexagonal", "clean architecture", "ddd", "solid", "refator",
                "refactor", "monolito", "microservico", "desacopl", "design pattern")) {
            return EngineeringIntent.ARCHITECTURE_AND_REFACTOR;
        }
        return EngineeringIntent.GENERAL_ENGINEERING;
    }

    public static List<String> detectStacks(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }
        List<String> stacks = new ArrayList<>();
        if (containsAny(normalizedText, "java", "spring", "quarkus", "micronaut", "maven", "gradle", "jvm")) {
            stacks.add("Java / JVM");
        }
        if (containsAny(normalizedText, "typescript", "javascript", "angular", "react", "nextjs", "next.js",
                "vue", "svelte", "nodejs", "node.js", "nestjs", "express", "deno", "bun")) {
            stacks.add("TypeScript / JavaScript");
        }
        if (containsAny(normalizedText, "python", "django", "fastapi", "flask", "pydantic", "sqlalchemy", "pytest")) {
            stacks.add("Python");
        }
        if (containsAny(normalizedText, "rust", "cargo", "tokio", "axum", "actix", "tauri")) {
            stacks.add("Rust");
        }
        if (containsAny(normalizedText, "golang", " go ", "gin", "fiber", "goroutine")) {
            stacks.add("Go");
        }
        if (containsAny(normalizedText, "c#", "csharp", "dotnet", ".net", "asp.net", "entity framework")) {
            stacks.add("C# / .NET");
        }
        if (containsAny(normalizedText, "kotlin", "ktor", "jetpack compose", "android", "coroutines")) {
            stacks.add("Kotlin");
        }
        if (containsAny(normalizedText, "php", "laravel", "symfony", "composer")) {
            stacks.add("PHP");
        }
        if (containsAny(normalizedText, "ruby", "rails", "rspec")) {
            stacks.add("Ruby");
        }
        if (containsAny(normalizedText, "dart", "flutter")) {
            stacks.add("Dart / Flutter");
        }
        if (containsAny(normalizedText, "c++", "cpp", "cmake", "clang", "gcc")) {
            stacks.add("C / C++");
        }
        if (containsAny(normalizedText, "swift", "swiftui", "ios")) {
            stacks.add("Swift");
        }
        if (containsAny(normalizedText, "sql", "postgres", "pgvector", "mysql", "sqlite")) {
            stacks.add("SQL / Database");
        }
        return List.copyOf(stacks);
    }

    private static String buildPlaybookSection(EngineeringIntent intent, List<String> stacks) {
        String basePlaybook = switch (intent) {
            case DEBUG_AND_FIX -> PLAYBOOK_DEBUG;
            case TESTING_AND_QA -> PLAYBOOK_TESTING;
            case SECURITY_AND_AUTH -> PLAYBOOK_SECURITY;
            case DATABASE_AND_DATA -> PLAYBOOK_DATABASE;
            case ARCHITECTURE_AND_REFACTOR -> PLAYBOOK_ARCHITECTURE;
            case GENERAL_ENGINEERING -> PLAYBOOK_GENERAL;
        };
        if (stacks.isEmpty()) {
            return basePlaybook.trim();
        }
        return basePlaybook.trim()
                + "\n- Ecossistema detectado na consulta: " + String.join(", ", stacks)
                + " (aplique convencoes e boas praticas idiomaticas dessa stack).";
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public enum EngineeringIntent {
        DEBUG_AND_FIX,
        TESTING_AND_QA,
        SECURITY_AND_AUTH,
        DATABASE_AND_DATA,
        ARCHITECTURE_AND_REFACTOR,
        GENERAL_ENGINEERING
    }

    public record RouteProfile(
            EngineeringIntent intent,
            List<String> detectedStacks,
            String playbookPrompt
    ) {
    }
}
