package io.github.jvpgjava.creatingj.bootstrap.config.security;

import io.github.jvpgjava.creatingj.bootstrap.config.web.ProblemDetailsFactory;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;
    private final ClockPort clock;
    private final int maxRequests;
    private final Duration window;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper,
            ClockPort clock,
            @Value("${creatingj.security.rate-limit.auth-max-requests:60}") int maxRequests,
            @Value("${creatingj.security.rate-limit.auth-window:1m}") Duration window
    ) {
        this.problemDetailsFactory = problemDetailsFactory;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith(AUTH_PATH_PREFIX) && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            String key = resolveClientKey(request) + ":" + path;
            if (!tryConsume(key)) {
                ProblemDetail problem = problemDetailsFactory.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        ErrorCode.TOO_MANY_REQUESTS,
                        "Limite de requisicoes excedido. Tente novamente em instantes.",
                        request
                );
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), problem);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(String key) {
        Instant now = clock.now();
        Bucket updated = buckets.compute(key, (k, existing) -> {
            if (existing == null || !existing.windowEndsAt().isAfter(now)) {
                return new Bucket(1, now.plus(window));
            }
            return new Bucket(existing.count() + 1, existing.windowEndsAt());
        });
        return updated.count() <= maxRequests;
    }

    private static String resolveClientKey(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    private record Bucket(int count, Instant windowEndsAt) {
    }
}
