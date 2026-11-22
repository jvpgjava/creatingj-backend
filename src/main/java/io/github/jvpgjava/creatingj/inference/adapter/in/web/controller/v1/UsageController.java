package io.github.jvpgjava.creatingj.inference.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.apidocs.v1.UsageApiDocs;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UsageSummaryResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.mapper.v1.UsageHttpMapper;
import io.github.jvpgjava.creatingj.inference.application.port.in.GetOwnUsageSummaryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usage")
public final class UsageController implements UsageApiDocs {

    private final GetOwnUsageSummaryUseCase getOwnUsageSummary;
    private final UsageHttpMapper mapper;

    public UsageController(GetOwnUsageSummaryUseCase getOwnUsageSummary, UsageHttpMapper mapper) {
        this.getOwnUsageSummary = getOwnUsageSummary;
        this.mapper = mapper;
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<UsageSummaryResponse> getOwnUsage(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(mapper.toSummaryResponse(getOwnUsageSummary.execute(actor.id())));
    }
}
