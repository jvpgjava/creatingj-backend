package io.github.jvpgjava.creatingj.identity.adapter.in.web.security;

import io.github.jvpgjava.creatingj.identity.application.port.in.ResolveAuthenticatedSessionUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public final class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_COOKIE_NAME = "CREATINGJ_SESSION";
    public static final String AUTH_TRANSPORT_ATTR = "creatingj.auth.transport";

    private static final String BEARER_PREFIX = "Bearer ";

    private final ResolveAuthenticatedSessionUseCase resolveSession;

    public SessionAuthenticationFilter(ResolveAuthenticatedSessionUseCase resolveSession) {
        this.resolveSession = resolveSession;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            ExtractedToken extracted = extractToken(request);
            if (extracted != null) {
                resolveSession.execute(extracted.token()).ifPresent(principal -> {
                    AuthenticatedUser user = new AuthenticatedUser(
                            principal.sessionId(),
                            principal.userId(),
                            principal.email(),
                            principal.roles()
                    );
                    List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .toList();
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    request.setAttribute(AUTH_TRANSPORT_ATTR, extracted.transport());
                });
            }
        }
        filterChain.doFilter(request, response);
    }

    private static ExtractedToken extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return new ExtractedToken(token, "BEARER");
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())
                        && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return new ExtractedToken(cookie.getValue().trim(), "COOKIE");
                }
            }
        }
        return null;
    }

    private record ExtractedToken(String token, String transport) {
    }
}
