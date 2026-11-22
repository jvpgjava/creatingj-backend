package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface ExchangePkceCodeUseCase {

    TokenPairResult execute(ExchangePkceCodeCommand command);

    record ExchangePkceCodeCommand(
            String code,
            String clientId,
            String redirectUri,
            String codeVerifier
    ) {
    }

    record TokenPairResult(
            UUID sessionId,
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            UserId userId,
            String email,
            Set<Role> roles
    ) {
    }
}
