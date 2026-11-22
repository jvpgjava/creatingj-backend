package io.github.jvpgjava.creatingj.bootstrap;

import io.github.jvpgjava.creatingj.bootstrap.config.security.AuthRateLimitFilter;
import io.github.jvpgjava.creatingj.bootstrap.config.security.CsrfCookieProtectionFilter;
import io.github.jvpgjava.creatingj.bootstrap.config.web.ProblemDetailsFactory;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.SessionAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfAndRateLimitFilterTest {

    private final ProblemDetailsFactory problemDetailsFactory = new ProblemDetailsFactory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectMutatingCookieRequestWithoutMatchingCsrfHeader() throws Exception {
        var filter = new CsrfCookieProtectionFilter(problemDetailsFactory, objectMapper);

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        request.setAttribute(SessionAuthenticationFilter.AUTH_TRANSPORT_ATTR, "COOKIE");
        request.setCookies(new Cookie("XSRF-TOKEN", "expected-csrf-token"));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
    }

    @Test
    void shouldAllowMutatingCookieRequestWithMatchingCsrfHeaderAndAllowBearer() throws Exception {
        var filter = new CsrfCookieProtectionFilter(problemDetailsFactory, objectMapper);

        var cookieRequest = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        cookieRequest.setAttribute(SessionAuthenticationFilter.AUTH_TRANSPORT_ATTR, "COOKIE");
        cookieRequest.setCookies(new Cookie("XSRF-TOKEN", "expected-csrf-token"));
        cookieRequest.addHeader("X-XSRF-TOKEN", "expected-csrf-token");
        var cookieResponse = new MockHttpServletResponse();

        filter.doFilter(cookieRequest, cookieResponse, new MockFilterChain());
        assertThat(cookieResponse.getStatus()).isEqualTo(200);

        var bearerRequest = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        bearerRequest.setAttribute(SessionAuthenticationFilter.AUTH_TRANSPORT_ATTR, "BEARER");
        var bearerResponse = new MockHttpServletResponse();

        filter.doFilter(bearerRequest, bearerResponse, new MockFilterChain());
        assertThat(bearerResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldThrottleExcessiveAuthRequestsAndResetAfterWindow() throws Exception {
        var now = new AtomicReference<>(Instant.parse("2026-11-21T23:55:00Z"));
        ClockPort clock = now::get;
        var filter = new AuthRateLimitFilter(problemDetailsFactory, objectMapper, clock, 2, Duration.ofMinutes(1));

        for (int i = 0; i < 2; i++) {
            var req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr("198.51.100.10");
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }

        var blockedReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blockedReq.setRemoteAddr("198.51.100.10");
        var blockedRes = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertThat(blockedRes.getStatus()).isEqualTo(429);
        assertThat(blockedRes.getContentAsString()).contains("TOO_MANY_REQUESTS");

        now.set(now.get().plusSeconds(61));
        var allowedAgainReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        allowedAgainReq.setRemoteAddr("198.51.100.10");
        var allowedAgainRes = new MockHttpServletResponse();
        filter.doFilter(allowedAgainReq, allowedAgainRes, new MockFilterChain());
        assertThat(allowedAgainRes.getStatus()).isEqualTo(200);
    }
}
