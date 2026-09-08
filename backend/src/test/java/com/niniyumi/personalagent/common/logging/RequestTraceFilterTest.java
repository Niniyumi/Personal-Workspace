package com.niniyumi.personalagent.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTraceFilterTest {
    @Test
    void addsOneServerGeneratedTraceIdToTheRequestResponseAndLogs() throws Exception {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "untrusted-client-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceSeenByApplication = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                traceSeenByApplication.set(RequestTraceContext.current()));

        assertThat(traceSeenByApplication.get()).isNotBlank().isNotEqualTo("untrusted-client-value");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceSeenByApplication.get());
        assertThat(MDC.get(RequestTraceContext.TRACE_ID)).isNull();
    }
}
