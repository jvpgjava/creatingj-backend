package io.github.jvpgjava.creatingj.bootstrap.config.provider;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@ConfigurationProperties(prefix = "creatingj.provider.ai")
public final class AiProviderProperties {

    private static final String DEFAULT_BASE_URL = "https://api.trystac.com/v1";
    private static final String DEFAULT_CHAT_MODEL = "z-ai/glm-5.3-flash";

    private final String baseUrl;
    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;
    private final boolean requireApiKey;
    private final boolean strictUrlValidation;

    public AiProviderProperties(
            @DefaultValue(DEFAULT_BASE_URL) String baseUrl,
            @DefaultValue("") String apiKey,
            @DefaultValue(DEFAULT_CHAT_MODEL) String chatModel,
            @DefaultValue("") String embeddingModel,
            @DefaultValue("false") boolean requireApiKey,
            @DefaultValue("true") boolean strictUrlValidation
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel == null ? "" : embeddingModel;
        this.requireApiKey = requireApiKey;
        this.strictUrlValidation = strictUrlValidation;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String chatModel() {
        return chatModel;
    }

    public String embeddingModel() {
        return embeddingModel;
    }

    public boolean requireApiKey() {
        return requireApiKey;
    }

    public boolean strictUrlValidation() {
        return strictUrlValidation;
    }

    @PostConstruct
    public void validateSecurityPolicy() {
        if (requireApiKey && apiKey.isBlank()) {
            throw new IllegalStateException("CRJ_API_KEY obrigatoria no perfil atual.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("CRJ_API_BASE_URL obrigatoria.");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("URL do provedor de IA invalida.", ex);
        }
        if (!strictUrlValidation) {
            return;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("URL do provedor de IA deve usar esquema HTTPS em modo estrito.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("URL do provedor de IA sem host valido.");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal")) {
            throw new IllegalStateException("Host interno nao permitido para URL do provedor de IA.");
        }
        rejectPrivateOrReservedLiteralIp(stripIpv6Brackets(normalizedHost));
    }

    private static String stripIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static void rejectPrivateOrReservedLiteralIp(String host) {
        if (!looksLikeIpLiteral(host)) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IllegalStateException(
                        "Endereco IP privado/reservado nao permitido para URL do provedor de IA.");
            }
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Host IP invalido para URL do provedor de IA.", ex);
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
        return "AiProviderProperties{"
                + "baseUrl='" + baseUrl + '\''
                + ", apiKey='[REDACTED]'"
                + ", chatModel='" + chatModel + '\''
                + ", embeddingModel='" + embeddingModel + '\''
                + ", requireApiKey=" + requireApiKey
                + ", strictUrlValidation=" + strictUrlValidation
                + '}';
    }
}
