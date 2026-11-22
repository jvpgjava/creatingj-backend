package io.github.jvpgjava.creatingj.maintenance.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.maintenance.adapter.in.web.apidocs.v1.AdminMaintenanceApiDocs;
import io.github.jvpgjava.creatingj.maintenance.adapter.in.web.dto.v1.DataRetentionReportResponse;
import io.github.jvpgjava.creatingj.maintenance.application.port.in.ExecuteDataRetentionUseCase;
import io.github.jvpgjava.creatingj.maintenance.domain.DataRetentionReport;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/maintenance")
public final class AdminMaintenanceController implements AdminMaintenanceApiDocs {

    private final ExecuteDataRetentionUseCase executeDataRetention;

    public AdminMaintenanceController(ExecuteDataRetentionUseCase executeDataRetention) {
        this.executeDataRetention = executeDataRetention;
    }

    @Override
    @PostMapping("/retention")
    public ResponseEntity<DataRetentionReportResponse> executeRetention(
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        DataRetentionReport report = executeDataRetention.execute(actor.id());
        return ResponseEntity.ok(new DataRetentionReportResponse(
                report.executedAt(),
                report.deletedSessions(),
                report.deletedPairingChallenges(),
                report.deletedCompletedTasks(),
                report.deletedArchivedConversations(),
                report.deletedAuditEvents(),
                report.totalDeleted()
        ));
    }
}
