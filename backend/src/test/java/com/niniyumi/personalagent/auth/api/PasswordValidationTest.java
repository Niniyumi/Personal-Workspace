package com.niniyumi.personalagent.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.auth.application.AuthService;
import com.niniyumi.personalagent.auth.application.PasswordResetService;
import com.niniyumi.personalagent.auth.application.RefreshTokenService;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtProperties;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtTokenService;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthService.class)
class PasswordValidationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private PasswordResetService passwordResetService;

    @ParameterizedTest
    @MethodSource("validBoundaryPasswords")
    void registerAcceptsPasswordsAtSeventyTwoUtf8Bytes(String password) throws Exception {
        when(passwordEncoder.encode(password)).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new User(42L, user.username(), user.email(), user.passwordHash(), user.displayName(),
                    user.status(), user.createdAt(), user.updatedAt());
        });

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(password)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @MethodSource("overlongPasswords")
    void registerRejectsPasswordsOverSeventyTwoUtf8Bytes(String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(password)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @ParameterizedTest
    @MethodSource("overlongPasswords")
    void loginRejectsPasswordsOverSeventyTwoUtf8BytesBeforeLookupOrEncoding(String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("login", "nini", "password", password))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    private String registerBody(String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", "nini",
                "email", "nini@example.com",
                "password", password,
                "displayName", "Nini"));
    }

    private static Stream<String> validBoundaryPasswords() {
        return Stream.of("a".repeat(72), "你".repeat(24));
    }

    private static Stream<String> overlongPasswords() {
        return Stream.of("a".repeat(73), "你".repeat(25));
    }
}
