package io.github.jvpgjava.creatingj.inference.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.AdminUsageOverview;

public interface GetAdminUsageOverviewUseCase {

    AdminUsageOverview execute(UserId actorUserId);
}
