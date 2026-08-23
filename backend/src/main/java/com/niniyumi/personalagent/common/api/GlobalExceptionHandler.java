package com.niniyumi.personalagent.common.api;

import com.niniyumi.personalagent.auth.application.EmailAlreadyExistsException;
import com.niniyumi.personalagent.auth.application.UsernameAlreadyExistsException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameAlreadyExists() {
        return error(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Username already exists");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists() {
        return error(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already exists");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, UUID.randomUUID().toString()));
    }
}
