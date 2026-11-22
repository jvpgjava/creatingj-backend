package io.github.jvpgjava.creatingj.bootstrap;

import io.github.jvpgjava.creatingj.bootstrap.config.security.ConversationRateLimitFilter;
import io.github.jvpgjava.creatingj.bootstrap.config.web.ProblemDetailsFactory;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationRateLimitFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRateLimitConversationMessagesPerAuthenticatedAccountAndResetAfterWindow() throws Exception {
        var now = new AtomicReference<>(Instant.parse("2026-11-22T14:00:00Z"));
        ClockPort clock = now::get;
        var filter = new ConversationRateLimitFilter(
                new ProblemDetailsFactory(), new ObjectMapper(), clock, 2, Duration.ofMinutes(1));

        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.randomUUID(), UserId.newId(), "alice@creatingj.dev", Set.of(Role.MEMBER));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        for (int i = 0; i < 2; i++) {
            var req = new MockHttpServletRequest("POST", "/api/v1/conversations/123/messages");
            var res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }

        var blockedReq = new MockHttpServletRequest("POST", "/api/v1/conversations/123/messages/stream");
        var blockedRes = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertThat(blockedRes.getStatus()).isEqualTo(429);
        assertThat(blockedRes.getContentAsString()).contains("TOO_MANY_REQUESTS");

        now.set(now.get().plusSeconds(61));
        var resetReq = new MockHttpServletRequest("POST", "/api/v1/conversations/123/messages");
        var resetRes = new MockHttpServletResponse();
        filter.doFilter(resetReq, resetRes, new MockFilterChain());
        assertThat(resetRes.getStatus()).isEqualTo(200);
    }
}
