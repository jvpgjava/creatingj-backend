package io.github.jvpgjava.creatingj.inference.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.UsageSummary;

public interface GetOwnUsageSummaryUseCase {

    UsageSummary execute(UserId actorUserId);
}
