package com.niniyumi.personalagent.auth.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.auth.infrastructure.security.ApiSecurityErrorHandler;
import com.niniyumi.personalagent.auth.infrastructure.security.SecurityConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = CurrentUserController.class, properties = "app.security.jwt-secret=test-jwt-secret-for-webmvc-tests-01")
@Import({CurrentUserController.class, SecurityConfig.class, ApiSecurityErrorHandler.class})
class CurrentUserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void currentUserRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void currentUserRejectsInvalidBearerTokenWithStructuredError() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void currentUserReturnsProfileForAuthenticatedPrincipal() throws Exception {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user()));

        mockMvc.perform(get("/api/users/me?id=99")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(42L, "nini"), "token", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("nini"));
    }

    @Test
    void currentUserRejectsDisabledUser() throws Exception {
        when(userRepository.findById(42L)).thenReturn(Optional.of(disabledUser()));

        mockMvc.perform(get("/api/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(42L, "nini"), "token", List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CURRENT_USER_DISABLED"))
                .andExpect(jsonPath("$.message").value("Current user is disabled"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void currentUserReturnsStructuredNotFoundWhenAccountNoLongerExists() throws Exception {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(42L, "nini"), "token", List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CURRENT_USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Current user not found"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private User user() {
        return new User(42L, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.ACTIVE, null, null);
    }

    private User disabledUser() {
        return new User(42L, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.DISABLED, null, null);
    }
}
