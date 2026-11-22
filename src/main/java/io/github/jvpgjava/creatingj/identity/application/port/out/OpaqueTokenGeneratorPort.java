package io.github.jvpgjava.creatingj.identity.application.port.out;

public interface OpaqueTokenGeneratorPort {

    String generateToken();

    String hash(String rawToken);
}
