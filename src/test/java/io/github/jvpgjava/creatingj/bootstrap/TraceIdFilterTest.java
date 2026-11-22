package io.github.jvpgjava.creatingj.bootstrap;

import io.github.jvpgjava.creatingj.bootstrap.config.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void shouldGenerateTraceIdAndCleanMdcAfterRequest() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/system/status");
        var response = new MockHttpServletResponse();
        var observedMdc = new AtomicReference<String>();

        FilterChain chain = (req, res) -> observedMdc.set(MDC.get(TraceIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(responseHeader).isNotBlank();
        assertThat(observedMdc.get()).isEqualTo(responseHeader);
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldPreserveValidIncomingTraceId() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/system/status");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "req-trace-12345678");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("req-trace-12345678");
    }

    @Test
    void shouldDiscardMaliciousOrMalformedTraceIdHeader() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/system/status");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "bad\r\ninjected-log-line<script>");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String responseHeader = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(responseHeader)
                .isNotBlank()
                .doesNotContain("\r")
                .doesNotContain("\n")
                .doesNotContain("<script>");
    }
}
