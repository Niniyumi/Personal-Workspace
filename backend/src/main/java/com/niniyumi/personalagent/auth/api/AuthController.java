package com.niniyumi.personalagent.auth.api;

import com.niniyumi.personalagent.auth.api.dto.RegisterRequest;
import com.niniyumi.personalagent.auth.api.dto.AuthTokensResponse;
import com.niniyumi.personalagent.auth.api.dto.LoginRequest;
import com.niniyumi.personalagent.auth.api.dto.RefreshRequest;
import com.niniyumi.personalagent.auth.api.dto.UserResponse;
import com.niniyumi.personalagent.auth.application.AuthService;
import com.niniyumi.personalagent.auth.application.RegisterCommand;
import com.niniyumi.personalagent.auth.application.LoginCommand;
import com.niniyumi.personalagent.auth.application.LoginResult;
import com.niniyumi.personalagent.auth.domain.User;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(new RegisterCommand(
                request.username(), request.email(), request.password(), request.displayName()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.id(), user.username(), user.email(), user.displayName()));
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

    private AuthTokensResponse tokens(LoginResult result) {
        return new AuthTokensResponse(result.accessToken(), result.refreshToken(), "Bearer", 900);
    }
}
