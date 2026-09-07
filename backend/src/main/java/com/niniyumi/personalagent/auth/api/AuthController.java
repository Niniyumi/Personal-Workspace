package com.niniyumi.personalagent.auth.api;

import com.niniyumi.personalagent.auth.api.dto.RegisterRequest;
import com.niniyumi.personalagent.auth.api.dto.AuthTokensResponse;
import com.niniyumi.personalagent.auth.api.dto.LoginRequest;
import com.niniyumi.personalagent.auth.api.dto.RefreshRequest;
import com.niniyumi.personalagent.auth.api.dto.UserResponse;
import com.niniyumi.personalagent.auth.api.dto.PasswordResetRequest;
import com.niniyumi.personalagent.auth.api.dto.PasswordResetConfirmRequest;
import com.niniyumi.personalagent.auth.api.dto.RegistrationVerificationRequest;
import com.niniyumi.personalagent.auth.application.AuthService;
import com.niniyumi.personalagent.auth.application.PasswordResetService;
import com.niniyumi.personalagent.auth.application.RegistrationVerificationService;
import com.niniyumi.personalagent.auth.application.RegisterCommand;
import com.niniyumi.personalagent.auth.application.LoginCommand;
import com.niniyumi.personalagent.auth.application.LoginResult;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final PasswordResetService passwordResetService;
    private final RegistrationVerificationService registrationVerificationService;

    public AuthController(AuthService authService, JwtProperties jwtProperties,
            PasswordResetService passwordResetService,
            RegistrationVerificationService registrationVerificationService) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.passwordResetService = passwordResetService;
        this.registrationVerificationService = registrationVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(new RegisterCommand(
                request.username(), request.email(), request.password(), request.displayName(), request.code()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.id(), user.username(), user.email(), user.displayName()));
    }

    @PostMapping("/registration-code/request")
    public ResponseEntity<Void> requestRegistrationCode(
            @Valid @RequestBody RegistrationVerificationRequest request) {
        registrationVerificationService.request(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
        return tokens(authService.login(new LoginCommand(request.login(), request.password())));
    }

    @PostMapping("/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return tokens(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request.email(), request.code(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private AuthTokensResponse tokens(LoginResult result) {
        return new AuthTokensResponse(result.accessToken(), result.refreshToken(), "Bearer",
                Math.multiplyExact(jwtProperties.accessTokenMinutes(), 60L));
    }
}
