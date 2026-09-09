package com.niniyumi.personalagent.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    private static final String BATCH_INDEX_HEADER = "X-Batch-Index";
    private static final String BATCH_TOTAL_HEADER = "X-Batch-Total";
    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        RequestTraceContext.set(traceId);
        response.setHeader(TRACE_HEADER, traceId);
        boolean apiRequest = request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        long startedAt = System.nanoTime();
        String batch = batchProgress(request);
        Exception failure = null;
        try {
            if (apiRequest) {
                if (batch == null) {
                    log.info("API request started, method={}, path={}",
                            request.getMethod(), request.getRequestURI());
                } else {
                    log.info("API request started, method={}, path={}, batch={}",
                            request.getMethod(), request.getRequestURI(), batch);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            failure = exception;
            throw exception;
        } finally {
            if (apiRequest) {
                long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
                logResult(request, response, batch, durationMs, failure);
            }
            RequestTraceContext.clear();
        }
    }

    private void logResult(HttpServletRequest request, HttpServletResponse response, String batch,
            long durationMs, Exception failure) {
        if (failure != null) {
            String reason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            log.error("API request crashed, method={}, path={}, exception={}, reason={}, durationMs={}",
                    request.getMethod(), request.getRequestURI(), failure.getClass().getSimpleName(),
                    reason, durationMs, failure);
            return;
        }
        if (response.getStatus() >= 400) {
            if (batch == null) {
                log.warn("API request failed, method={}, path={}, status={}, durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            } else {
                log.warn("API request failed, method={}, path={}, status={}, batch={}, durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), batch, durationMs);
            }
            return;
        }
        if (batch == null) {
            log.info("API request completed, method={}, path={}, status={}, durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
        } else {
            log.info("API request completed, method={}, path={}, status={}, batch={}, durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), batch, durationMs);
        }
    }

    private String batchProgress(HttpServletRequest request) {
        String index = positiveIntegerHeader(request.getHeader(BATCH_INDEX_HEADER));
        String total = positiveIntegerHeader(request.getHeader(BATCH_TOTAL_HEADER));
        return index == null || total == null ? null : index + "/" + total;
    }

    private String positiveIntegerHeader(String value) {
        if (value == null || !value.matches("[1-9]\\d*")) {
            return null;
        }
        return value;
    }
}
