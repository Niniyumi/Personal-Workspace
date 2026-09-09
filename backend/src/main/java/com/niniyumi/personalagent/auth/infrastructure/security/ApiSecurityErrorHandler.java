package com.niniyumi.personalagent.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.common.api.ApiErrorResponse;
import com.niniyumi.personalagent.common.logging.RequestTraceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiSecurityErrorHandler.class);
    private final ObjectMapper objectMapper;

    public ApiSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException, ServletException {
        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        // 安全过滤器异常不会进入 RestControllerAdvice，因此在过滤器层直接输出统一结构。
        log.warn("API security request rejected, status={}, code={}, reason={}",
                status.value(), code, message);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(code, message, RequestTraceContext.currentOrCreate()));
    }
}
