package io.github.jvpgjava.creatingj.common;

import io.github.jvpgjava.creatingj.agent.domain.ProposedToolAction;
import io.github.jvpgjava.creatingj.agent.domain.ToolExecutionPolicyEngine;
import io.github.jvpgjava.creatingj.agent.domain.ToolPolicyDecision;
import io.github.jvpgjava.creatingj.common.domain.CodeSecurityAnalyzer;
import io.github.jvpgjava.creatingj.common.domain.EngineeringIntentRouter;
import io.github.jvpgjava.creatingj.common.domain.EngineeringIntentRouter.EngineeringIntent;
import io.github.jvpgjava.creatingj.common.domain.OutputSecurityGuard;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.knowledge.domain.MultiQueryRagDecomposer;
import io.github.jvpgjava.creatingj.knowledge.domain.TextChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeAiGoldenEvalTest {

    @Test
    void shouldChunkCodeWithClassAndFunctionContextAcrossJavaPythonTypeScriptAndRust() {
        String javaCode = "public class OrderService {\n"
                + "    public void processPayment(String orderId) {\n"
                + ("        validateOrderLine(orderId);\n").repeat(14)
                + "    }\n}\n";
        List<String> javaChunks = TextChunker.split(javaCode, 280, 40);
        assertThat(javaChunks).hasSizeGreaterThan(1);
        assertThat(javaChunks.get(1)).contains("[Codigo: OrderService > processPayment]");

        String pythonCode = "class AuthController:\n"
                + "    async def verify_token(self, token: str):\n"
                + ("        await check_revocation_store(token)\n").repeat(14);
        List<String> pyChunks = TextChunker.split(pythonCode, 280, 40);
        assertThat(pyChunks).hasSizeGreaterThan(1);
        assertThat(pyChunks.get(1)).contains("[Codigo: AuthController > verify_token]");

        String rustCode = "impl RunnerClient {\n"
                + "    pub async fn lease_next_task(&self) {\n"
                + ("        self.poll_heartbeat().await;\n").repeat(14)
                + "    }\n}\n";
        List<String> rsChunks = TextChunker.split(rustCode, 280, 40);
        assertThat(rsChunks).hasSizeGreaterThan(1);
        assertThat(rsChunks.get(1)).contains("[Codigo: RunnerClient > lease_next_task]");

        String tsCode = "export class DashboardComponent {\n"
                + "    export function renderMetrics() {\n"
                + ("        updateChartSeries();\n").repeat(14)
                + "    }\n}\n";
        List<String> tsChunks = TextChunker.split(tsCode, 280, 40);
        assertThat(tsChunks).hasSizeGreaterThan(1);
        assertThat(tsChunks.get(1)).contains("[Codigo: DashboardComponent > renderMetrics]");
    }

    @Test
    void shouldRouteEngineeringIntentsAndDetectAllProgrammingLanguagesAndFrameworks() {
        var debugJava = EngineeringIntentRouter.route(
                "Estou recebendo NullPointerException e stacktrace no Spring Boot e Maven"
        );
        assertThat(debugJava.intent()).isEqualTo(EngineeringIntent.DEBUG_AND_FIX);
        assertThat(debugJava.detectedStacks()).contains("Java / JVM");
        assertThat(debugJava.playbookPrompt()).contains("DEBUG_AND_FIX").contains("Java / JVM");

        var testTs = EngineeringIntentRouter.route(
                "Como criar testes unitarios com Vitest e Playwright em um projeto Angular e React com TypeScript?"
        );
        assertThat(testTs.intent()).isEqualTo(EngineeringIntent.TESTING_AND_QA);
        assertThat(testTs.detectedStacks()).contains("TypeScript / JavaScript");

        var secPy = EngineeringIntentRouter.route(
                "Revisar vulnerabilidade de SQL injection e autenticacao JWT no FastAPI e Django em Python"
        );
        assertThat(secPy.intent()).isEqualTo(EngineeringIntent.SECURITY_AND_AUTH);
        assertThat(secPy.detectedStacks()).contains("Python");

        var dbSql = EngineeringIntentRouter.route(
                "Otimizar migration Flyway e indice HNSW no PostgreSQL com pgvector e query SQL"
        );
        assertThat(dbSql.intent()).isEqualTo(EngineeringIntent.DATABASE_AND_DATA);
        assertThat(dbSql.detectedStacks()).contains("SQL / Database");

        var archMulti = EngineeringIntentRouter.route(
                "Aplicar arquitetura hexagonal e SOLID em Rust com Tokio, Go com Gin, C# .NET, Kotlin, "
                        + "PHP Laravel, Ruby on Rails, Flutter Dart, C++ CMake e Swift iOS"
        );
        assertThat(archMulti.intent()).isEqualTo(EngineeringIntent.ARCHITECTURE_AND_REFACTOR);
        assertThat(archMulti.detectedStacks()).contains(
                "Rust", "Go", "C# / .NET", "Kotlin", "PHP", "Ruby", "Dart / Flutter", "C / C++", "Swift"
        );

        var general = EngineeringIntentRouter.route("Como estruturar uma funcao recursiva simples?");
        assertThat(general.intent()).isEqualTo(EngineeringIntent.GENERAL_ENGINEERING);
        assertThat(EngineeringIntentRouter.route(null).intent()).isEqualTo(EngineeringIntent.GENERAL_ENGINEERING);
    }

    @Test
    void shouldDecomposeCompoundGoalsFuseMultiQueryRagAndExpandCutCodeNeighbors() {
        List<String> decomposed = MultiQueryRagDecomposer.decompose(
                "Revisar autenticacao OAuth2 PKCE; verificar migration do pgvector, e criar teste de CORS"
        );
        assertThat(decomposed).hasSize(3);
        assertThat(MultiQueryRagDecomposer.decompose("Consulta simples")).hasSize(1);
        assertThat(MultiQueryRagDecomposer.decompose(" ")).isEmpty();

        var s1 = KnowledgeSourceId.random();
        var s2 = KnowledgeSourceId.random();
        var m1 = new KnowledgeSearchMatch(s1, "src/Auth.java", "Auth", 0,
                "public class Auth {\n    public void login() {", 0.82, "v1");
        var m1Next = new KnowledgeSearchMatch(s1, "src/Auth.java", "Auth", 1,
                "        verifyToken();\n    }\n}", 0.68, "v1");
        var m2 = new KnowledgeSearchMatch(s2, "src/Cors.java", "Cors", 0,
                "```java\npublic class CorsFilter {", 0.79, "v1");
        var m2Next = new KnowledgeSearchMatch(s2, "src/Cors.java", "Cors", 1,
                "}\n```", 0.64, "v1");

        List<KnowledgeSearchMatch> fused = MultiQueryRagDecomposer.fuseMultiQueryResults(
                List.of(List.of(m1, m2), List.of(m2, m1)),
                4
        );
        assertThat(fused).hasSize(2);

        List<KnowledgeSearchMatch> expanded = MultiQueryRagDecomposer.expandCutCodeNeighbors(
                fused,
                List.of(m1, m1Next, m2, m2Next),
                4
        );
        assertThat(expanded).hasSize(4);
        assertThat(MultiQueryRagDecomposer.looksLikeCutCodeOrSection("```ts\nconst x = 1;")).isTrue();
        assertThat(MultiQueryRagDecomposer.looksLikeCutCodeOrSection("function a() { if (true) {")).isTrue();
        assertThat(MultiQueryRagDecomposer.looksLikeCutCodeOrSection("function a() { return 1; }")).isFalse();
        assertThat(MultiQueryRagDecomposer.looksLikeCutCodeOrSection(" ")).isFalse();
    }

    @Test
    void shouldDetectInsecureCodePatternsAcrossLanguagesAndBlockCriticalRunnerDiffs() {
        var sqlConcat = CodeSecurityAnalyzer.analyze(
                "String q = \"SELECT * FROM users WHERE email = '\" + userEmail;"
        );
        assertThat(sqlConcat.flags()).contains("insecure_code_pattern:sql_concatenation");
        assertThat(sqlConcat.hasCriticalFinding()).isTrue();

        var evalJs = CodeSecurityAnalyzer.analyze("const res = eval(userInput);");
        assertThat(evalJs.flags()).contains("insecure_code_pattern:dangerous_eval");
        assertThat(evalJs.hasCriticalFinding()).isTrue();

        var xssHtml = CodeSecurityAnalyzer.analyze("element.innerHTML = commentText;");
        assertThat(xssHtml.flags()).contains("insecure_code_pattern:xss_raw_html_sink");

        var tlsOffPy = CodeSecurityAnalyzer.analyze("requests.get(url, verify=False)");
        assertThat(tlsOffPy.flags()).contains("insecure_code_pattern:tls_verification_disabled");
        assertThat(tlsOffPy.hasCriticalFinding()).isTrue();

        var weakHash = CodeSecurityAnalyzer.analyze("MessageDigest.getInstance(\"MD5\")");
        assertThat(weakHash.flags()).contains("insecure_code_pattern:weak_password_hash");

        var unsafeDeser = CodeSecurityAnalyzer.analyze("data = pickle.loads(payload)");
        assertThat(unsafeDeser.flags()).contains("insecure_code_pattern:insecure_deserialization");
        assertThat(unsafeDeser.hasCriticalFinding()).isTrue();

        var sanitizedWithAdvisory = OutputSecurityGuard.inspectAndSanitize(
                "Exemplo legado:\n```python\nrequests.get(url, verify=False)\n```",
                true
        );
        assertThat(sanitizedWithAdvisory.flags()).contains("insecure_code_pattern:tls_verification_disabled");
        assertThat(sanitizedWithAdvisory.content()).contains("[Revisao Automatica de Seguranca CreatingJ]");

        var deniedDiff = ToolExecutionPolicyEngine.evaluateWithDiff(
                new ProposedToolAction("WRITE_FILE", "src/Repo.java", null, "Query"),
                "+ String sql = \"SELECT * FROM accounts WHERE id = '\" + inputId;"
        );
        assertThat(deniedDiff.policyDecision()).isEqualTo(ToolPolicyDecision.DENIED_BY_POLICY);
    }

    @Test
    void shouldAllowSafeTestAndBuildCommandsAcrossAllMajorLanguagesAndFrameworks() {
        List<String> universalSafeCommands = List.of(
                "mvn test",
                "./mvnw -B verify",
                "gradle check",
                "./gradlew test",
                "npm test",
                "npm run typecheck",
                "pnpm lint",
                "yarn build",
                "npx vitest run",
                "npx jest",
                "npx tsc --noEmit",
                "npx ng test",
                "deno test",
                "bun test",
                "pytest",
                "python3 -m pytest",
                "ruff check",
                "mypy",
                "cargo test",
                "cargo clippy",
                "go test ./...",
                "go vet ./...",
                "dotnet test",
                "composer test",
                "php artisan test",
                "bundle exec rspec",
                "flutter test",
                "dart analyze",
                "ctest",
                "mix test",
                "swift test",
                "git status"
        );

        for (String cmd : universalSafeCommands) {
            var eval = ToolExecutionPolicyEngine.evaluate(
                    new ProposedToolAction("RUN_COMMAND", null, cmd, "Verify build/tests")
            );
            assertThat(eval.policyDecision())
                    .as("Expected safe multi-language command to require approval (not denied): %s", cmd)
                    .isEqualTo(ToolPolicyDecision.REQUIRES_APPROVAL);
        }
    }
}
