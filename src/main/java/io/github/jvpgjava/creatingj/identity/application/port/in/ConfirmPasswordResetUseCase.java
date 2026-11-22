package io.github.jvpgjava.creatingj.identity.application.port.in;

public interface ConfirmPasswordResetUseCase {

    void execute(ConfirmPasswordResetCommand command);

    record ConfirmPasswordResetCommand(
            String token,
            String newRawPassword
    ) {
    }
}
