package io.github.jvpgjava.creatingj.knowledge.adapter.out.embedding;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.EmbeddingPort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenAiCompatibleEmbeddingAdapter implements EmbeddingPort {

    private final URI embeddingsUri;
    private final String apiKey;
    private final String model;
    private final int expectedDimensions;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleEmbeddingAdapter(
            String baseUrl,
            String apiKey,
            String model,
            int expectedDimensions,
            Duration timeout,
            ObjectMapper objectMapper
    ) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.embeddingsUri = URI.create(normalizedBase + "/embeddings");
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model.trim();
        this.expectedDimensions = expectedDimensions;
        this.timeout = timeout == null ? Duration.ofSeconds(15) : timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String embeddingVersion() {
        return "openai:" + model + ":" + expectedDimensions;
    }

    @Override
    public EmbeddingVector embed(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Texto para embedding obrigatorio.");
        }
        if (apiKey.isBlank() || model.isBlank()) {
            throw new DomainException(
                    ErrorCode.PROVIDER_UNAVAILABLE,
                    "Provedor de embeddings nao configurado para o perfil atual."
            );
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", text);
        body.put("dimensions", expectedDimensions);
        String payload = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder(embeddingsUri)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException ex) {
            throw new DomainException(ErrorCode.PROVIDER_TIMEOUT, "Tempo limite excedido ao gerar embeddings.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DomainException(ErrorCode.PROVIDER_UNAVAILABLE, "Requisicao de embeddings interrompida.");
        } catch (IOException ex) {
            throw new DomainException(
                    ErrorCode.PROVIDER_UNAVAILABLE,
                    "Falha de comunicacao com provedor de embeddings."
            );
        }

        int status = response.statusCode();
        if (status == 429) {
            throw new DomainException(ErrorCode.PROVIDER_RATE_LIMITED, "Provedor de embeddings limitou requisicoes.");
        }
        if (status == 401 || status == 403 || status >= 500) {
            throw new DomainException(
                    ErrorCode.PROVIDER_UNAVAILABLE,
                    "Provedor de embeddings indisponivel no momento."
            );
        }
        if (status < 200 || status >= 300) {
            throw new DomainException(ErrorCode.BAD_GATEWAY, "Resposta invalida do provedor de embeddings.");
        }
        return parseEmbeddingResponse(response.body());
    }

    private EmbeddingVector parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode vectorNode = root.path("data").path(0).path("embedding");
            if (!vectorNode.isArray() || vectorNode.size() != expectedDimensions) {
                throw new DomainException(
                        ErrorCode.BAD_GATEWAY,
                        "Dimensao de embedding incompativel com o indice configurado."
                );
            }
            float[] values = new float[expectedDimensions];
            for (int i = 0; i < values.length; i++) {
                values[i] = (float) vectorNode.get(i).asDouble();
            }
            return EmbeddingVector.ofExpectedDimensions(values, expectedDimensions);
        } catch (DomainException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCode.BAD_GATEWAY, "Formato invalido na resposta do provedor de embeddings.");
        }
    }
}
