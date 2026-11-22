package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase.TokenPairResult;

public interface RefreshSessionUseCase {

    TokenPairResult execute(RefreshSessionCommand command);

    record RefreshSessionCommand(String refreshToken) {
    }
}
