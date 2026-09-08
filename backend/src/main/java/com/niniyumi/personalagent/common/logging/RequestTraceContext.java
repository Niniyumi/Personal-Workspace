package com.niniyumi.personalagent.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

public final class RequestTraceContext {
    public static final String TRACE_ID = "traceId";

    private RequestTraceContext() {
    }

    public static void set(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static String current() {
        return MDC.get(TRACE_ID);
    }

    public static String currentOrCreate() {
        String traceId = current();
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
