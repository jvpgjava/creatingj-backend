package io.github.jvpgjava.creatingj.inference;

import com.sun.net.httpserver.HttpServer;
import io.github.jvpgjava.creatingj.bootstrap.config.provider.AiProviderProperties;
import io.github.jvpgjava.creatingj.inference.adapter.out.provider.OpenAiCompatibleModelGatewayAdapter;
import io.github.jvpgjava.creatingj.inference.domain.ChatRole;
import io.github.jvpgjava.creatingj.inference.domain.GenerationChunk;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;
import io.github.jvpgjava.creatingj.inference.domain.ModelProviderException;
import io.github.jvpgjava.creatingj.inference.domain.PromptMessage;
import io.github.jvpgjava.creatingj.inference.domain.ProviderFailureKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleModelGatewayTest {

    private static final String FAKE_SECRET = "crj-secret-key-must-never-leak-99999";

    private HttpServer server;
    private AtomicInteger nextStatus;
    private AtomicReference<String> nextBody;
    private AtomicReference<String> nextContentType;
    private AtomicInteger artificialDelayMs;
    private OpenAiCompatibleModelGatewayAdapter adapter;

    @BeforeEach
    void startMockServer() throws Exception {
        nextStatus = new AtomicInteger(200);
        nextBody = new AtomicReference<>("");
        nextContentType = new AtomicReference<>("application/json");
        artificialDelayMs = new AtomicInteger(0);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            byte[] payload = """
                    {"data":[{"id":"qwen/qwen3.5-9b"},{"id":"z-ai/glm-5.3-flash"}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.createContext("/v1/chat/completions", exchange -> {
            int delay = artificialDelayMs.get();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] payload = nextBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", nextContentType.get());
            exchange.sendResponseHeaders(nextStatus.get(), payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
        var props = new AiProviderProperties(baseUrl, FAKE_SECRET, "z-ai/glm-5.3-flash", "", false, false);
        adapter = new OpenAiCompatibleModelGatewayAdapter(
                props, new ObjectMapper(), Duration.ofSeconds(2), Duration.ofSeconds(2));
    }

    @AfterEach
    void stopMockServer() {
        server.stop(0);
    }

    @Test
    void shouldListModelsAndExecuteCompleteAndStreamSkippingEmptyReasoningChunks() {
        assertThat(adapter.listAvailableModels()).contains("z-ai/glm-5.3-flash");

        nextBody.set("""
                {
                  "model": "z-ai/glm-5.3-flash",
                  "choices": [{"message": {"role": "assistant", "content": "Ola mundo"}, "finish_reason": "stop"}],
                  "usage": {"prompt_tokens": 11, "completion_tokens": 7, "total_tokens": 18}
                }
                """);
        var req = new GenerationRequest(
                "z-ai/glm-5.3-flash", List.of(new PromptMessage(ChatRole.USER, "Oi")), 512, 0.2);

        ModelCompletionResult completed = adapter.complete(req);
        assertThat(completed.content()).isEqualTo("Ola mundo");
        assertThat(completed.usage().totalTokens()).isEqualTo(18);

        nextContentType.set("text/event-stream");
        nextBody.set("""
                data: {"model":"z-ai/glm-5.3-flash","choices":[{"delta":{"content":"","reasoning":"pensando"}}]}

                data: {"model":"z-ai/glm-5.3-flash","choices":[{"delta":{"content":"Ola "}}]}

                data: {"model":"z-ai/glm-5.3-flash","choices":[{"delta":{"content":"SSE!"},"finish_reason":"stop"}]}

                data: {"model":"z-ai/glm-5.3-flash","choices":[],"usage":\
                {"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}

                data: [DONE]

                """);

        List<GenerationChunk> chunks = new ArrayList<>();
        ModelCompletionResult streamed = adapter.stream(req, chunks::add, () -> false);
        assertThat(streamed.content()).isEqualTo("Ola SSE!");
        assertThat(streamed.usage().totalTokens()).isEqualTo(15);
        assertThat(chunks).extracting(GenerationChunk::deltaText).containsExactly("Ola ", "SSE!");
    }

    @Test
    void shouldSupportStreamCancellationAndSanitizeProviderErrorsWithoutLeakingApiKey() {
        var req = new GenerationRequest(
                "z-ai/glm-5.3-flash", List.of(new PromptMessage(ChatRole.USER, "Oi")), 512, 0.2);

        nextContentType.set("text/event-stream");
        nextBody.set("""
                data: {"choices":[{"delta":{"content":"Primeiro"}}]}

                data: {"choices":[{"delta":{"content":"Segundo"}}]}

                data: [DONE]

                """);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        assertThatThrownBy(() -> adapter.stream(req, chunk -> cancelled.set(true), cancelled::get))
                .isInstanceOf(ModelProviderException.class)
                .extracting(e -> ((ModelProviderException) e).failureKind())
                .isEqualTo(ProviderFailureKind.CANCELLED);

        nextContentType.set("application/json");
        nextStatus.set(401);
        nextBody.set("{\"error\":\"invalid api key " + FAKE_SECRET + "\"}");
        assertThatThrownBy(() -> adapter.complete(req))
                .isInstanceOf(ModelProviderException.class)
                .satisfies(ex -> {
                    var mpe = (ModelProviderException) ex;
                    assertThat(mpe.failureKind()).isEqualTo(ProviderFailureKind.UNAUTHORIZED);
                    assertThat(mpe.getMessage()).doesNotContain(FAKE_SECRET);
                });

        nextStatus.set(429);
        nextBody.set("{\"error\":\"rate limit exceeded\"}");
        assertThatThrownBy(() -> adapter.complete(req))
                .isInstanceOf(ModelProviderException.class)
                .extracting(e -> ((ModelProviderException) e).failureKind())
                .isEqualTo(ProviderFailureKind.RATE_LIMITED);

        nextStatus.set(503);
        nextBody.set("internal upstream failure");
        assertThatThrownBy(() -> adapter.complete(req))
                .isInstanceOf(ModelProviderException.class)
                .extracting(e -> ((ModelProviderException) e).failureKind())
                .isEqualTo(ProviderFailureKind.UNAVAILABLE);

        nextStatus.set(200);
        nextBody.set("{not-json");
        assertThatThrownBy(() -> adapter.complete(req))
                .isInstanceOf(ModelProviderException.class)
                .extracting(e -> ((ModelProviderException) e).failureKind())
                .isEqualTo(ProviderFailureKind.BAD_RESPONSE);

        nextBody.set("{\"choices\":[]}");
        artificialDelayMs.set(3000);
        assertThatThrownBy(() -> adapter.complete(req))
                .isInstanceOf(ModelProviderException.class)
                .extracting(e -> ((ModelProviderException) e).failureKind())
                .isEqualTo(ProviderFailureKind.TIMEOUT);
    }
}
