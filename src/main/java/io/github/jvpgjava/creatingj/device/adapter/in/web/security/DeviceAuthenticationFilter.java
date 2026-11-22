package io.github.jvpgjava.creatingj.device.adapter.in.web.security;

import io.github.jvpgjava.creatingj.device.application.port.in.ResolveAuthenticatedDeviceUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public final class DeviceAuthenticationFilter extends OncePerRequestFilter {

    public static final String DEVICE_TOKEN_HEADER = "X-Device-Token";

    private final ResolveAuthenticatedDeviceUseCase resolveDevice;

    public DeviceAuthenticationFilter(ResolveAuthenticatedDeviceUseCase resolveDevice) {
        this.resolveDevice = resolveDevice;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/v1/runner/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawToken = request.getHeader(DEVICE_TOKEN_HEADER);
            if (rawToken != null && !rawToken.isBlank()) {
                resolveDevice.execute(rawToken).ifPresent(principal -> {
                    AuthenticatedDevice device = new AuthenticatedDevice(
                            principal.deviceId(),
                            principal.ownerUserId(),
                            principal.credentialId()
                    );
                    var auth = new UsernamePasswordAuthenticationToken(
                            device,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_RUNNER"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
