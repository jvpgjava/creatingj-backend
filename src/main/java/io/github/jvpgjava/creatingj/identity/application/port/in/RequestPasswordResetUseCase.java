package io.github.jvpgjava.creatingj.identity.application.port.in;

import java.util.Optional;

public interface RequestPasswordResetUseCase {

    PasswordResetRequestResult execute(RequestPasswordResetCommand command);

    record RequestPasswordResetCommand(String email) {
    }

    record PasswordResetRequestResult(Optional<String> issuedResetToken) {
    }
}
