package io.github.jvpgjava.creatingj.inference.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;

import java.util.Optional;

public interface UserQuotaStorePort {

    Optional<UserQuota> findByUserId(UserId userId);

    UserQuota save(UserQuota quota);
}
