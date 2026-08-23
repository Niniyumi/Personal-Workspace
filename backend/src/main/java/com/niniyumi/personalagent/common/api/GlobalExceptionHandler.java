package com.niniyumi.personalagent.common.api;

import com.niniyumi.personalagent.auth.application.EmailAlreadyExistsException;
import com.niniyumi.personalagent.auth.application.CurrentUserDisabledException;
import com.niniyumi.personalagent.auth.application.CurrentUserNotFoundException;
import com.niniyumi.personalagent.auth.application.InvalidCredentialsException;
import com.niniyumi.personalagent.auth.application.InvalidRefreshTokenException;
import com.niniyumi.personalagent.auth.application.UsernameAlreadyExistsException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCurrentUserNotFound() {
        return error(HttpStatus.NOT_FOUND, "CURRENT_USER_NOT_FOUND", "Current user not found");
    }

    @ExceptionHandler(CurrentUserDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleCurrentUserDisabled() {
        return error(HttpStatus.FORBIDDEN, "CURRENT_USER_DISABLED", "Current user is disabled");
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameAlreadyExists() {
        return error(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Username already exists");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists() {
        return error(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already exists");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken() {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, UUID.randomUUID().toString()));
    }
}
