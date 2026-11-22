package io.github.jvpgjava.creatingj.common.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.common.adapter.in.web.dto.v1.SystemStatusResponse;
import io.github.jvpgjava.creatingj.common.domain.SystemStatus;
import org.springframework.stereotype.Component;

@Component
public final class SystemStatusHttpMapper {

    public SystemStatusResponse toResponse(SystemStatus status) {
        return new SystemStatusResponse(
                status.service(),
                status.apiVersion(),
                status.status(),
                status.timestamp()
        );
    }
}
