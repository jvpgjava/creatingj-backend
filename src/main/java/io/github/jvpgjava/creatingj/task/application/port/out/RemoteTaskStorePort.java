package io.github.jvpgjava.creatingj.task.application.port.out;

import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RemoteTaskStorePort {

    RemoteTask save(RemoteTask task);

    Optional<RemoteTask> findById(TaskId taskId);

    Optional<RemoteTask> findByIdempotency(UserId ownerUserId, DeviceId deviceId, String idempotencyKey);

    List<RemoteTask> findRecentByOwner(UserId ownerUserId, int limit);

    Optional<RemoteTask> findNextLeasableForDevice(DeviceId deviceId, Instant now);
}
