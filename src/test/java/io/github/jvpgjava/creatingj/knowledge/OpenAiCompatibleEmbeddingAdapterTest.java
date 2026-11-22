package io.github.jvpgjava.creatingj.knowledge;

import com.sun.net.httpserver.HttpServer;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding.OpenAiCompatibleEmbeddingAdapter;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleEmbeddingAdapterTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldEmbedValid384VectorAndSanitizeUpstreamErrors() throws Exception {
        AtomicInteger statusCode = new AtomicInteger(200);
        AtomicReference<String> responseBody = new AtomicReference<>(valid384Response());

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            byte[] payload = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode.get(), payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();

        String secretKey = "crj-super-secret-embedding-key";
        OpenAiCompatibleEmbeddingAdapter adapter = new OpenAiCompatibleEmbeddingAdapter(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                secretKey,
                "text-embedding-3-small",
                Duration.ofSeconds(5),
                new ObjectMapper()
        );

        assertThat(adapter.embeddingVersion()).isEqualTo("openai:text-embedding-3-small:384");
        EmbeddingVector vector = adapter.embed("hello pgvector");
        assertThat(vector.dimensions()).isEqualTo(EmbeddingVector.EXPECTED_DIMENSIONS);

        responseBody.set("{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}");
        assertThatThrownBy(() -> adapter.embed("wrong dim"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.errorCode()).isEqualTo(ErrorCode.BAD_GATEWAY);
                    assertThat(de.getMessage()).doesNotContain(secretKey);
                });

        statusCode.set(429);
        responseBody.set("{\"detail\":\"rate limit with " + secretKey + "\"}");
        assertThatThrownBy(() -> adapter.embed("rate limited"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.errorCode()).isEqualTo(ErrorCode.PROVIDER_RATE_LIMITED);
                    assertThat(de.getMessage()).doesNotContain(secretKey);
                });

        statusCode.set(503);
        responseBody.set("{\"detail\":\"stac embeddings offline\"}");
        assertThatThrownBy(() -> adapter.embed("unavailable"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.errorCode()).isEqualTo(ErrorCode.PROVIDER_UNAVAILABLE);
                    assertThat(de.getMessage()).doesNotContain("stac embeddings offline");
                });
    }

    private static String valid384Response() {
        StringBuilder sb = new StringBuilder("{\"data\":[{\"embedding\":[");
        for (int i = 0; i < EmbeddingVector.EXPECTED_DIMENSIONS; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i == 0 ? "1.0" : "0.0");
        }
        sb.append("]}]}");
        return sb.toString();
    }
}
