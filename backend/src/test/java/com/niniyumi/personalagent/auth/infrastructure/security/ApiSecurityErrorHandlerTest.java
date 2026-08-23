package com.niniyumi.personalagent.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.common.api.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class ApiSecurityErrorHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiSecurityErrorHandler handler = new ApiSecurityErrorHandler(objectMapper);

    @Test
    void accessDeniedProducesStructuredForbiddenResponseWithoutExceptionDetails() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("sensitive detail"));

        ApiErrorResponse error = objectMapper.readValue(response.getContentAsByteArray(), ApiErrorResponse.class);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(error.code()).isEqualTo("FORBIDDEN");
        assertThat(error.message()).isEqualTo("Access is denied");
        assertThat(error.traceId()).isNotBlank();
        assertThat(response.getContentAsString()).doesNotContain("sensitive detail");
    }
}
