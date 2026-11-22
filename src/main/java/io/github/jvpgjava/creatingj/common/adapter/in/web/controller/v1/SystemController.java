package io.github.jvpgjava.creatingj.common.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.common.adapter.in.web.apidocs.v1.SystemApiDocs;
import io.github.jvpgjava.creatingj.common.adapter.in.web.dto.v1.SystemStatusResponse;
import io.github.jvpgjava.creatingj.common.adapter.in.web.mapper.v1.SystemStatusHttpMapper;
import io.github.jvpgjava.creatingj.common.application.port.in.GetSystemStatusUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public final class SystemController implements SystemApiDocs {

    private final GetSystemStatusUseCase getSystemStatus;
    private final SystemStatusHttpMapper mapper;

    public SystemController(GetSystemStatusUseCase getSystemStatus, SystemStatusHttpMapper mapper) {
        this.getSystemStatus = getSystemStatus;
        this.mapper = mapper;
    }

    @Override
    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getStatus() {
        return ResponseEntity.ok(mapper.toResponse(getSystemStatus.execute()));
    }
}
