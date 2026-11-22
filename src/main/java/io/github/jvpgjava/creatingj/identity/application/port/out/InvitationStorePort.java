package io.github.jvpgjava.creatingj.identity.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Optional;

public interface InvitationStorePort {

    Invitation save(Invitation invitation);

    Optional<Invitation> findByTokenHash(String tokenHash);

    void invalidatePendingByUserAndPurpose(UserId userId, InvitationPurpose purpose, Instant now);
}
