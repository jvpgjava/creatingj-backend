package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.Map;
import java.util.Set;

public final class PublicClientRegistry {

    private static final Map<String, Set<String>> ALLOWED_CLIENTS = Map.of(
            "creatingj-vscode", Set.of(
                    "vscode://jvpgjava.creatingj-vscode/auth-callback",
                    "http://127.0.0.1/callback"
            ),
            "creatingj-cli", Set.of(
                    "http://127.0.0.1/callback",
                    "http://127.0.0.1:49152/callback"
            ),
            "creatingj-desktop", Set.of(
                    "creatingj-desktop://auth/callback",
                    "http://127.0.0.1/callback"
            ),
            "creatingj-mobile", Set.of(
                    "creatingj://auth/callback"
            )
    );

    private PublicClientRegistry() {
    }

    public static void validate(String clientId, String redirectUri) {
        if (clientId == null || clientId.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "clientId e redirectUri sao obrigatorios.");
        }
        Set<String> allowedRedirects = ALLOWED_CLIENTS.get(clientId.trim());
        if (allowedRedirects == null || !allowedRedirects.contains(redirectUri.trim())) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Cliente ou redirectUri nao autorizado.");
        }
    }
}
