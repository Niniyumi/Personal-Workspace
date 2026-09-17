package com.niniyumi.personalagent.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void logsApiRequestStartAndCompletionWithBatchProgress(CapturedOutput output) throws Exception {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/weekly-reports/import-docx");
        request.addHeader("X-Batch-Index", "2");
        request.addHeader("X-Batch-Total", "5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> response.setStatus(502));

        assertThat(output)
                .contains("API request started, method=POST, path=/api/weekly-reports/import-docx, batch=2/5")
                .contains("API request failed, method=POST, path=/api/weekly-reports/import-docx, status=502, batch=2/5")
                .contains("durationMs=");
    }

    @Test
    void logsUnhandledApiExceptionInsteadOfReportingSuccess(CapturedOutput output) {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/courses/7/complete");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("upstream failed");
                }))
                .isInstanceOf(ServletException.class)
                .hasMessage("upstream failed");

        assertThat(output)
                .contains("API request crashed, method=POST, path=/api/courses/7/complete")
                .contains("exception=ServletException")
                .contains("reason=upstream failed")
                .contains("durationMs=")
                .doesNotContain("API request completed");
        assertThat(MDC.get(RequestTraceContext.TRACE_ID)).isNull();
    }

    @Test
    void masksTemporaryPlaybackTicketInRequestLogs(CapturedOutput output) throws Exception {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/course-audio/temporary-secret-ticket");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> response.setStatus(200));

        assertThat(output)
                .contains("path=/api/course-audio/***")
                .doesNotContain("temporary-secret-ticket");
    }
}
