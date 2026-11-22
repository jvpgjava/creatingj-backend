package io.github.jvpgjava.creatingj.inference.application.usecase;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.application.port.in.GetAdminUsageOverviewUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.domain.AdminUsageOverview;

import java.time.Instant;

public final class GetAdminUsageOverviewInteractor implements GetAdminUsageOverviewUseCase {

    private final UserStorePort userStore;
    private final UsageRecordStorePort usageStore;
    private final ClockPort clock;

    public GetAdminUsageOverviewInteractor(
            UserStorePort userStore,
            UsageRecordStorePort usageStore,
            ClockPort clock
    ) {
        this.userStore = userStore;
        this.usageStore = usageStore;
        this.clock = clock;
    }

    @Override
    public AdminUsageOverview execute(UserId actorUserId) {
        User actor = userStore.findById(actorUserId)
                .filter(u -> u.isActive() && u.isAdmin())
                .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN, "Acesso administrativo negado."));

        Instant now = clock.now();
        UtcDayWindow window = UtcDayWindow.containing(now);
        long totalTokens = usageStore.sumAllTokensBetween(window.startInclusive(), window.endExclusive());
        long totalRequests = usageStore.countAllRequestsBetween(window.startInclusive(), window.endExclusive());
        return new AdminUsageOverview(window.startInclusive(), window.endExclusive(), totalTokens, totalRequests);
    }
}
