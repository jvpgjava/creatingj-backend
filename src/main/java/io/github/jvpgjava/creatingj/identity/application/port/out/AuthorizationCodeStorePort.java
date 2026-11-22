package io.github.jvpgjava.creatingj.identity.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;

import java.util.Optional;

public interface AuthorizationCodeStorePort {

    AuthorizationCode save(AuthorizationCode code);

    Optional<AuthorizationCode> findByCodeHash(String codeHash);
}
