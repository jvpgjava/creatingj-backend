package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;

public interface AuthorizePkceUseCase {

    PkceAuthorizationResult execute(AuthorizePkceCommand command);

    record AuthorizePkceCommand(
            UserId actorUserId,
            String clientId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String state
    ) {
    }

    record PkceAuthorizationResult(
            String authorizationCode,
            String redirectUri,
            String state,
            Instant expiresAt
    ) {
    }
}
