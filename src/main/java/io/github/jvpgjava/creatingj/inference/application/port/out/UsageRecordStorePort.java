package io.github.jvpgjava.creatingj.inference.application.port.out;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;

import java.time.Instant;
import java.util.List;

public interface UsageRecordStorePort {

    UsageRecord save(UsageRecord record);

    long sumTokensByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive);

    long countRequestsByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive);

    List<UsageRecord> findRecentByUser(UserId userId, int limit);

    long sumAllTokensBetween(Instant startInclusive, Instant endExclusive);

    long countAllRequestsBetween(Instant startInclusive, Instant endExclusive);
}
