package io.github.jvpgjava.creatingj.identity.application.port.in;

public interface ActivateAccountUseCase {

    void execute(ActivateAccountCommand command);

    record ActivateAccountCommand(
            String token,
            String rawPassword
    ) {
    }
}
