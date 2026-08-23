package com.niniyumi.personalagent.auth.api;

import com.niniyumi.personalagent.auth.api.dto.RegisterRequest;
import com.niniyumi.personalagent.auth.api.dto.UserResponse;
import com.niniyumi.personalagent.auth.application.AuthService;
import com.niniyumi.personalagent.auth.application.RegisterCommand;
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
}
