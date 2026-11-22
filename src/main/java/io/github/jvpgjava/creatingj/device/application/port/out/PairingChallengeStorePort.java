package io.github.jvpgjava.creatingj.device.application.port.out;

import io.github.jvpgjava.creatingj.device.domain.PairingChallenge;

import java.util.Optional;

public interface PairingChallengeStorePort {

    PairingChallenge save(PairingChallenge challenge);

    Optional<PairingChallenge> findByCodeHash(String codeHash);
}
