package io.github.jvpgjava.creatingj.inference.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;

public interface UpdateUserQuotaUseCase {

    UserQuota execute(UpdateUserQuotaCommand command);

    record UpdateUserQuotaCommand(
            UserId actorUserId,
            UserId targetUserId,
            long dailyTokenLimit,
            int dailyRequestLimit,
            int maxConcurrentGenerations
    ) {
    }
}
