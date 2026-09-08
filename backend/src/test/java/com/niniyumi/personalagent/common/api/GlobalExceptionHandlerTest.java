package com.niniyumi.personalagent.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.auth.application.VerificationMailSendException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {
    @AfterEach
    void clearTraceContext() {
        MDC.clear();
    }

    @Test
    void logsMailCauseWithTheSameTraceIdReturnedToTheClient(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MDC.put("traceId", "trace-mail-test");

        ResponseEntity<ApiErrorResponse> response = handler.handleVerificationMailSendFailure(
                new VerificationMailSendException(new IllegalStateException("smtp unavailable")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VERIFICATION_MAIL_SEND_FAILED");
        assertThat(response.getBody().traceId()).isEqualTo("trace-mail-test");
        assertThat(output).contains("trace-mail-test").contains("smtp unavailable");
    }

    @Test
    void returnsTraceableResponseForUnexpectedException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("database unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().traceId()).isNotBlank();
    }
}
