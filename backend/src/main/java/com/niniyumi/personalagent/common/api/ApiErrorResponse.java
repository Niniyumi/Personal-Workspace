package com.niniyumi.personalagent.common.api;

public record ApiErrorResponse(String code, String message, String traceId) {
}
