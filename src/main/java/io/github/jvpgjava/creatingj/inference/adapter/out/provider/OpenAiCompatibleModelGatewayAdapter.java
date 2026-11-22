package io.github.jvpgjava.creatingj.inference.adapter.out.provider;

import io.github.jvpgjava.creatingj.bootstrap.config.provider.AiProviderProperties;
import io.github.jvpgjava.creatingj.inference.application.port.out.ModelGatewayPort;
import io.github.jvpgjava.creatingj.inference.domain.GenerationChunk;
import io.github.jvpgjava.creatingj.inference.domain.GenerationRequest;
import io.github.jvpgjava.creatingj.inference.domain.ModelCompletionResult;
import io.github.jvpgjava.creatingj.inference.domain.ModelProviderException;
import io.github.jvpgjava.creatingj.inference.domain.ProviderFailureKind;
import io.github.jvpgjava.creatingj.inference.domain.TokenUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Component
public final class OpenAiCompatibleModelGatewayAdapter implements ModelGatewayPort {

    private final AiProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    @Autowired
    public OpenAiCompatibleModelGatewayAdapter(
            AiProviderProperties properties,
            ObjectMapper objectMapper,
            @Value("${creatingj.provider.ai.connect-timeout:10s}") Duration connectTimeout,
            @Value("${creatingj.provider.ai.request-timeout:60s}") Duration requestTimeout
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public ModelCompletionResult complete(GenerationRequest request) {
        String body = serializeChatPayload(request, false);
        HttpRequest httpRequest = buildPostRequest("/chat/completions", body);
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            verifySuccessStatus(response.statusCode());
            JsonNode root = parseJson(response.body());
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asString("");
            String finishReason = choice.path("finish_reason").asString("");
            String model = root.path("model").asString(request.modelId());
            TokenUsage usage = parseUsage(root.path("usage"));
            return new ModelCompletionResult(content, model, finishReason, usage);
        } catch (ModelProviderException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ModelProviderException(ProviderFailureKind.TIMEOUT, "Tempo limite excedido no provedor de IA.",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Requisicao interrompida.", ex);
        } catch (IOException ex) {
            throw new ModelProviderException(ProviderFailureKind.UNAVAILABLE, "Provedor de IA indisponivel.", ex);
        }
    }

    @Override
    public ModelCompletionResult stream(
            GenerationRequest request,
            Consumer<GenerationChunk> onChunk,
            BooleanSupplier cancelled
    ) {
        String body = serializeChatPayload(request, true);
        HttpRequest httpRequest = buildPostRequest("/chat/completions", body);
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            verifySuccessStatus(response.statusCode());

            StringBuilder assembled = new StringBuilder();
            String finishReason = "";
            String resolvedModel = request.modelId();
            TokenUsage usage = TokenUsage.empty();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled.getAsBoolean()) {
                        throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Geracao cancelada.");
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    JsonNode chunkNode = parseJson(payload);
                    if (chunkNode.hasNonNull("model")) {
                        resolvedModel = chunkNode.path("model").asString(resolvedModel);
                    }
                    if (chunkNode.hasNonNull("usage") && !chunkNode.path("usage").isNull()) {
                        usage = parseUsage(chunkNode.path("usage"));
                    }
                    JsonNode choices = chunkNode.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        JsonNode first = choices.get(0);
                        String delta = first.path("delta").path("content").asString("");
                        String chunkFinish = first.path("finish_reason").asString("");
                        if (!chunkFinish.isEmpty()) {
                            finishReason = chunkFinish;
                        }
                        if (!delta.isEmpty()) {
                            assembled.append(delta);
                            onChunk.accept(new GenerationChunk(delta, chunkFinish.isEmpty() ? null : chunkFinish,
                                    null));
                        }
                    }
                }
            }

            if (cancelled.getAsBoolean()) {
                throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Geracao cancelada.");
            }
            return new ModelCompletionResult(assembled.toString(), resolvedModel, finishReason, usage);
        } catch (ModelProviderException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ModelProviderException(ProviderFailureKind.TIMEOUT, "Tempo limite excedido no provedor de IA.",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Streaming interrompido.", ex);
        } catch (IOException ex) {
            throw new ModelProviderException(ProviderFailureKind.UNAVAILABLE, "Conexao com provedor de IA falhou.", ex);
        }
    }

    @Override
    public List<String> listAvailableModels() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri("/models"))
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + properties.apiKey())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            verifySuccessStatus(response.statusCode());
            JsonNode root = parseJson(response.body());
            List<String> models = new ArrayList<>();
            for (JsonNode item : root.path("data")) {
                String id = item.path("id").asString("");
                if (!id.isBlank()) {
                    models.add(id);
                }
            }
            return List.copyOf(models);
        } catch (ModelProviderException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ModelProviderException(ProviderFailureKind.TIMEOUT, "Tempo limite excedido no provedor de IA.",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelProviderException(ProviderFailureKind.CANCELLED, "Consulta de modelos interrompida.", ex);
        } catch (IOException ex) {
            throw new ModelProviderException(ProviderFailureKind.UNAVAILABLE, "Provedor de IA indisponivel.", ex);
        }
    }

    private HttpRequest buildPostRequest(String path, String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
    }

    private URI buildUri(String suffix) {
        String base = properties.baseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + suffix);
    }

    private String serializeChatPayload(GenerationRequest request, boolean stream) {
        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role().name().toLowerCase(Locale.ROOT), "content", m.content()))
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", request.modelId());
        payload.put("messages", messages);
        payload.put("max_tokens", request.maxTokens());
        payload.put("temperature", request.temperature());
        if (stream) {
            payload.put("stream", true);
            payload.put("stream_options", Map.of("include_usage", true));
        }
        return objectMapper.writeValueAsString(payload);
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (RuntimeException ex) {
            throw new ModelProviderException(ProviderFailureKind.BAD_RESPONSE,
                    "Resposta invalida recebida do provedor de IA.", ex);
        }
    }

    private static TokenUsage parseUsage(JsonNode usageNode) {
        if (usageNode == null || usageNode.isMissingNode() || usageNode.isNull()) {
            return TokenUsage.empty();
        }
        int prompt = usageNode.path("prompt_tokens").asInt(0);
        int completion = usageNode.path("completion_tokens").asInt(0);
        int total = usageNode.path("total_tokens").asInt(prompt + completion);
        return new TokenUsage(prompt, completion, total);
    }

    private static void verifySuccessStatus(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        if (statusCode == 401 || statusCode == 403) {
            throw new ModelProviderException(ProviderFailureKind.UNAUTHORIZED,
                    "Falha de autenticacao com o provedor de IA.");
        }
        if (statusCode == 429) {
            throw new ModelProviderException(ProviderFailureKind.RATE_LIMITED,
                    "Limite de taxa excedido no provedor de IA.");
        }
        if (statusCode >= 500) {
            throw new ModelProviderException(ProviderFailureKind.UNAVAILABLE,
                    "Provedor de IA temporariamente indisponivel.");
        }
        throw new ModelProviderException(ProviderFailureKind.BAD_RESPONSE,
                "Erro ao comunicar com o provedor de IA.");
    }
}
