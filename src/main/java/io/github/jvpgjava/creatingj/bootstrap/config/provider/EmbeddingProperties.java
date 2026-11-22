package io.github.jvpgjava.creatingj.bootstrap.config.provider;

import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@ConfigurationProperties(prefix = "creatingj.knowledge.embedding")
public final class EmbeddingProperties {

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimensions;
    private final int timeoutSeconds;
    private final boolean strictUrlValidation;

    public EmbeddingProperties(
            @DefaultValue("local-hash") String provider,
            @DefaultValue("https://api.trystac.com/v1") String baseUrl,
            @DefaultValue("") String apiKey,
            @DefaultValue("") String model,
            @DefaultValue("384") int dimensions,
            @DefaultValue("15") int timeoutSeconds,
            @DefaultValue("true") boolean strictUrlValidation
    ) {
        this.provider = provider == null ? "local-hash" : provider.trim().toLowerCase(Locale.ROOT);
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model.trim();
        this.dimensions = dimensions;
        this.timeoutSeconds = timeoutSeconds;
        this.strictUrlValidation = strictUrlValidation;
    }

    public String provider() {
        return provider;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public int dimensions() {
        return dimensions;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean strictUrlValidation() {
        return strictUrlValidation;
    }

    @PostConstruct
    public void validatePolicy() {
        if (dimensions != EmbeddingVector.EXPECTED_DIMENSIONS) {
            throw new IllegalStateException(
                    "Dimensao de embedding configurada deve ser " + EmbeddingVector.EXPECTED_DIMENSIONS + "."
            );
        }
        if (!"local-hash".equals(provider) && !"openai-compatible".equals(provider)) {
            throw new IllegalStateException("Provedor de embeddings nao suportado: " + provider);
        }
        if ("openai-compatible".equals(provider)) {
            validateBaseUrl();
        }
    }

    private void validateBaseUrl() {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("URL base do provedor de embeddings obrigatoria.");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("URL do provedor de embeddings invalida.", ex);
        }
        if (!strictUrlValidation) {
            return;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("URL do provedor de embeddings deve usar HTTPS em modo estrito.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("URL do provedor de embeddings sem host valido.");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal")) {
            throw new IllegalStateException("Host interno nao permitido para URL do provedor de embeddings.");
        }
        String bareHost = normalizedHost.startsWith("[") && normalizedHost.endsWith("]")
                ? normalizedHost.substring(1, normalizedHost.length() - 1)
                : normalizedHost;
        if (looksLikeIpLiteral(bareHost)) {
            try {
                InetAddress addr = InetAddress.getByName(bareHost);
                if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
                    throw new IllegalStateException("IP privado/reservado nao permitido para embeddings.");
                }
            } catch (UnknownHostException ex) {
                throw new IllegalStateException("Host IP invalido para embeddings.", ex);
            }
        }
    }

    private static boolean looksLikeIpLiteral(String host) {
        if (host.contains(":")) {
            return true;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if ((c < '0' || c > '9') && c != '.') {
                return false;
            }
        }
        return !host.isEmpty();
    }

    @Override
    public String toString() {
        return "EmbeddingProperties{"
                + "provider='" + provider + '\''
                + ", baseUrl='" + baseUrl + '\''
                + ", apiKey='[REDACTED]'"
                + ", model='" + model + '\''
                + ", dimensions=" + dimensions
                + ", timeoutSeconds=" + timeoutSeconds
                + ", strictUrlValidation=" + strictUrlValidation
                + '}';
    }
}
