package io.github.jvpgjava.creatingj.bootstrap.config.web;

import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
public final class ProblemDetailsFactory {

    private static final String ERROR_TYPE_PREFIX = "https://creatingj.dev/errors/";

    public ProblemDetail create(HttpStatus status, ErrorCode code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_TYPE_PREFIX + code.name().toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", resolveTraceId(request));
        return problem;
    }

    private static String resolveTraceId(HttpServletRequest request) {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        if (request != null) {
            Object attr = request.getAttribute(TraceIdFilter.MDC_KEY);
            if (attr instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }
}
