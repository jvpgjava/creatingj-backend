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

    private static final String DEFAULT_ONNX_MODEL =
            "https://huggingface.co/Xenova/multilingual-e5-base/resolve/main/onnx/model_quantized.onnx";
    private static final String DEFAULT_ONNX_TOKENIZER =
            "https://huggingface.co/Xenova/multilingual-e5-base/resolve/main/tokenizer.json";

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimensions;
    private final int timeoutSeconds;
    private final boolean strictUrlValidation;
    private final String onnxModelUri;
    private final String onnxTokenizerUri;
    private final String onnxCacheDir;
    private final String onnxModelOutputName;
    private final String onnxVersionTag;
    private final String onnxDocumentPrefix;
    private final String onnxQueryPrefix;

    public EmbeddingProperties(
            @DefaultValue("local-hash") String provider,
            @DefaultValue("https://api.trystac.com/v1") String baseUrl,
            @DefaultValue("") String apiKey,
            @DefaultValue("") String model,
            @DefaultValue("768") int dimensions,
            @DefaultValue("15") int timeoutSeconds,
            @DefaultValue("true") boolean strictUrlValidation,
            @DefaultValue(DEFAULT_ONNX_MODEL) String onnxModelUri,
            @DefaultValue(DEFAULT_ONNX_TOKENIZER) String onnxTokenizerUri,
            @DefaultValue("") String onnxCacheDir,
            @DefaultValue("last_hidden_state") String onnxModelOutputName,
            @DefaultValue("onnx:multilingual-e5-base-q8:768") String onnxVersionTag,
            @DefaultValue("passage: ") String onnxDocumentPrefix,
            @DefaultValue("query: ") String onnxQueryPrefix
    ) {
        this.provider = provider == null ? "local-hash" : provider.trim().toLowerCase(Locale.ROOT);
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model.trim();
        this.dimensions = dimensions;
        this.timeoutSeconds = timeoutSeconds;
        this.strictUrlValidation = strictUrlValidation;
        this.onnxModelUri = onnxModelUri == null ? DEFAULT_ONNX_MODEL : onnxModelUri.trim();
        this.onnxTokenizerUri = onnxTokenizerUri == null ? DEFAULT_ONNX_TOKENIZER : onnxTokenizerUri.trim();
        this.onnxCacheDir = onnxCacheDir == null ? "" : onnxCacheDir.trim();
        this.onnxModelOutputName = onnxModelOutputName == null || onnxModelOutputName.isBlank()
                ? "last_hidden_state" : onnxModelOutputName.trim();
        this.onnxVersionTag = onnxVersionTag == null || onnxVersionTag.isBlank()
                ? "onnx:multilingual-e5-base-q8:768" : onnxVersionTag.trim();
        this.onnxDocumentPrefix = onnxDocumentPrefix == null ? "" : onnxDocumentPrefix;
        this.onnxQueryPrefix = onnxQueryPrefix == null ? "" : onnxQueryPrefix;
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

    public String onnxModelUri() {
        return onnxModelUri;
    }

    public String onnxTokenizerUri() {
        return onnxTokenizerUri;
    }

    public String onnxCacheDir() {
        return onnxCacheDir;
    }

    public String onnxModelOutputName() {
        return onnxModelOutputName;
    }

    public String onnxVersionTag() {
        return onnxVersionTag;
    }

    public String onnxDocumentPrefix() {
        return onnxDocumentPrefix;
    }

    public String onnxQueryPrefix() {
        return onnxQueryPrefix;
    }

    @PostConstruct
    public void validatePolicy() {
        if (dimensions < EmbeddingVector.MIN_DIMENSIONS || dimensions > EmbeddingVector.MAX_DIMENSIONS) {
            throw new IllegalStateException(
                    "Dimensao de embedding deve estar entre " + EmbeddingVector.MIN_DIMENSIONS
                            + " e " + EmbeddingVector.MAX_DIMENSIONS + "."
            );
        }
        if (!"local-hash".equals(provider)
                && !"onnx-local".equals(provider)
                && !"openai-compatible".equals(provider)) {
            throw new IllegalStateException("Provedor de embeddings nao suportado: " + provider);
        }
        if ("openai-compatible".equals(provider)) {
            validateRemoteUrl(baseUrl, false);
        }
        if ("onnx-local".equals(provider)) {
            validateRemoteUrl(onnxModelUri, true);
            validateRemoteUrl(onnxTokenizerUri, true);
        }
    }

    private void validateRemoteUrl(String rawUrl, boolean allowFileScheme) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("URL do provedor de embeddings obrigatoria.");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("URL do provedor de embeddings invalida.", ex);
        }
        if (allowFileScheme && "file".equalsIgnoreCase(uri.getScheme())) {
            return;
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
                + ", onnxVersionTag='" + onnxVersionTag + '\''
                + '}';
    }
}
