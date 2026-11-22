package io.github.jvpgjava.creatingj.maintenance.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.maintenance.domain.DataRetentionReport;

public interface ExecuteDataRetentionUseCase {

    DataRetentionReport execute(UserId actorUserId);
}
