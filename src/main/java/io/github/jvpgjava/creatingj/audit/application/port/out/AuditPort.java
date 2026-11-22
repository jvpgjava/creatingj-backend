package io.github.jvpgjava.creatingj.audit.application.port.out;

import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;

public interface AuditPort {

    void record(AuditEvent event);
}
