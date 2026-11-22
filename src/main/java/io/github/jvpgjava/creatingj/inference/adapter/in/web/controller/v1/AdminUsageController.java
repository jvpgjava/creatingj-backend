package io.github.jvpgjava.creatingj.inference.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.apidocs.v1.AdminUsageApiDocs;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.AdminUsageOverviewResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UpdateUserQuotaRequest;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UserQuotaResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.mapper.v1.UsageHttpMapper;
import io.github.jvpgjava.creatingj.inference.application.port.in.GetAdminUsageOverviewUseCase;
import io.github.jvpgjava.creatingj.inference.application.port.in.UpdateUserQuotaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public final class AdminUsageController implements AdminUsageApiDocs {

    private final GetAdminUsageOverviewUseCase getAdminUsageOverview;
    private final UpdateUserQuotaUseCase updateUserQuota;
    private final UsageHttpMapper mapper;

    public AdminUsageController(
            GetAdminUsageOverviewUseCase getAdminUsageOverview,
            UpdateUserQuotaUseCase updateUserQuota,
            UsageHttpMapper mapper
    ) {
        this.getAdminUsageOverview = getAdminUsageOverview;
        this.updateUserQuota = updateUserQuota;
        this.mapper = mapper;
    }

    @Override
    @GetMapping("/usage")
    public ResponseEntity<AdminUsageOverviewResponse> getUsageOverview(
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return ResponseEntity.ok(mapper.toOverviewResponse(getAdminUsageOverview.execute(actor.id())));
    }

    @Override
    @PutMapping("/users/{userId}/quota")
    public ResponseEntity<UserQuotaResponse> updateUserQuota(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserQuotaRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var updated = updateUserQuota.execute(mapper.toUpdateQuotaCommand(actor.id(), userId, request));
        return ResponseEntity.ok(mapper.toQuotaResponse(updated));
    }
}
