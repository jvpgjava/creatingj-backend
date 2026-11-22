package io.github.jvpgjava.creatingj.inference.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.AdminUsageOverviewResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UpdateUserQuotaRequest;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UsageRecordResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UsageSummaryResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UserQuotaResponse;
import io.github.jvpgjava.creatingj.inference.application.port.in.UpdateUserQuotaUseCase.UpdateUserQuotaCommand;
import io.github.jvpgjava.creatingj.inference.domain.AdminUsageOverview;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UsageSummary;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;
import org.springframework.stereotype.Component;

@Component
public final class UsageHttpMapper {

    public UpdateUserQuotaCommand toUpdateQuotaCommand(
            UserId actorId,
            String targetUserIdRaw,
            UpdateUserQuotaRequest request
    ) {
        return new UpdateUserQuotaCommand(
                actorId,
                UserId.fromString(targetUserIdRaw),
                request.dailyTokenLimit(),
                request.dailyRequestLimit(),
                request.maxConcurrentGenerations()
        );
    }

    public UserQuotaResponse toQuotaResponse(UserQuota quota) {
        return new UserQuotaResponse(
                quota.userId().value().toString(),
                quota.dailyTokenLimit(),
                quota.dailyRequestLimit(),
                quota.maxConcurrentGenerations(),
                quota.updatedAt()
        );
    }

    public UsageSummaryResponse toSummaryResponse(UsageSummary summary) {
        return new UsageSummaryResponse(
                summary.windowStart(),
                summary.windowEnd(),
                summary.usedTokens(),
                summary.usedRequests(),
                summary.remainingTokens(),
                summary.remainingRequests(),
                toQuotaResponse(summary.quota()),
                summary.recentRecords().stream().map(this::toRecordResponse).toList()
        );
    }

    public UsageRecordResponse toRecordResponse(UsageRecord record) {
        return new UsageRecordResponse(
                record.id().toString(),
                record.conversationId() == null ? null : record.conversationId().toString(),
                record.generationId() == null ? null : record.generationId().toString(),
                record.modelId(),
                record.source().name(),
                record.promptTokens(),
                record.completionTokens(),
                record.totalTokens(),
                record.estimated(),
                record.latencyMs(),
                record.outcome().name(),
                record.recordedAt()
        );
    }

    public AdminUsageOverviewResponse toOverviewResponse(AdminUsageOverview overview) {
        return new AdminUsageOverviewResponse(
                overview.windowStart(),
                overview.windowEnd(),
                overview.totalTokensToday(),
                overview.totalRequestsToday()
        );
    }
}
