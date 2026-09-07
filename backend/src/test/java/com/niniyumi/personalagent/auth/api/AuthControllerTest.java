package com.niniyumi.personalagent.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niniyumi.personalagent.auth.application.AuthService;
import com.niniyumi.personalagent.auth.application.PasswordResetService;
import com.niniyumi.personalagent.auth.application.InvalidCredentialsException;
import com.niniyumi.personalagent.auth.application.InvalidRefreshTokenException;
import com.niniyumi.personalagent.auth.application.InvalidRegistrationVerificationCodeException;
import com.niniyumi.personalagent.auth.application.LoginCommand;
import com.niniyumi.personalagent.auth.application.LoginResult;
import com.niniyumi.personalagent.auth.application.RegisterCommand;
import com.niniyumi.personalagent.auth.application.RegistrationVerificationService;
import com.niniyumi.personalagent.auth.application.UsernameAlreadyExistsException;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private RegistrationVerificationService registrationVerificationService;

    @BeforeEach
    void setUp() {
        when(jwtProperties.accessTokenMinutes()).thenReturn(23L);
    }

    @Test
    void registerCreatesUser() throws Exception {
        when(authService.register(any(RegisterCommand.class))).thenReturn(user());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nini","email":"nini@example.com",
                                 "password":"UnitTest7!","displayName":"Nini","code":"123456"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("nini"));
    }

    @Test
    void requestsRegistrationCodeForEmail() throws Exception {
        mockMvc.perform(post("/api/auth/registration-code/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nini@example.com"}
                                """))
                .andExpect(status().isNoContent());

        verify(registrationVerificationService).request("nini@example.com");
    }

    @Test
    void registerRejectsBlankFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"","password":"","displayName":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsUsernameContainingAtSign() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nini@example.com","email":"nini@example.com",
                                 "password":"UnitTest7!","displayName":"Nini","code":"123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerMapsDuplicateUsernameToConflict() throws Exception {
        when(authService.register(any(RegisterCommand.class))).thenThrow(new UsernameAlreadyExistsException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nini","email":"nini@example.com",
                                 "password":"UnitTest7!","displayName":"Nini","code":"123456"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_EXISTS"));
    }

    @Test
    void registerMapsInvalidVerificationCodeToBadRequest() throws Exception {
        when(authService.register(any(RegisterCommand.class)))
                .thenThrow(new InvalidRegistrationVerificationCodeException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nini","email":"nini@example.com",
                                 "password":"UnitTest7!","displayName":"Nini","code":"654321"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_CODE"));
    }

    @Test
    void loginReturnsBothTokensUsingBearerContract() throws Exception {
        when(authService.login(any(LoginCommand.class))).thenReturn(new LoginResult("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nini","password":"UnitTest7!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(1_380));
    }

    @Test
    void loginMapsInvalidCredentialsToGenericUnauthorizedResponse() throws Exception {
        when(authService.login(any(LoginCommand.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nini","password":"UnitTest7!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshReturnsReplacementTokenPair() throws Exception {
        when(authService.refresh("used-token")).thenReturn(new LoginResult("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"used-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refreshMapsInvalidTokenToUnauthorizedResponse() throws Exception {
        when(authService.refresh("invalid-token")).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"invalid-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutReturnsNoContentForActiveSession() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"used-token"}
                                """))
                .andExpect(status().isNoContent());
    }

    private User user() {
        return new User(42L, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.ACTIVE, null, null);
    }
}
