package io.github.jvpgjava.creatingj.bootstrap.config.security;

import io.github.jvpgjava.creatingj.bootstrap.config.web.ProblemDetailsFactory;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthCookieWriter;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.SessionAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public final class CsrfCookieProtectionFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;

    public CsrfCookieProtectionFilter(ProblemDetailsFactory problemDetailsFactory, ObjectMapper objectMapper) {
        this.problemDetailsFactory = problemDetailsFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Object transport = request.getAttribute(SessionAuthenticationFilter.AUTH_TRANSPORT_ATTR);
        if ("COOKIE".equals(transport) && !SAFE_METHODS.contains(request.getMethod())) {
            String cookieToken = findCookieValue(request, AuthCookieWriter.XSRF_COOKIE_NAME);
            String headerToken = request.getHeader(CSRF_HEADER);
            if (!tokensMatch(cookieToken, headerToken)) {
                ProblemDetail problem = problemDetailsFactory.create(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FORBIDDEN,
                        "Token CSRF ausente ou invalido.",
                        request
                );
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), problem);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String findCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static boolean tokensMatch(String cookieToken, String headerToken) {
        if (cookieToken == null || cookieToken.isBlank() || headerToken == null || headerToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8),
                headerToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
