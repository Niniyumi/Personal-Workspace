package com.niniyumi.personalagent.common.api;

import com.niniyumi.personalagent.auth.application.EmailAlreadyExistsException;
import com.niniyumi.personalagent.auth.application.CurrentUserDisabledException;
import com.niniyumi.personalagent.auth.application.CurrentUserNotFoundException;
import com.niniyumi.personalagent.auth.application.InvalidCredentialsException;
import com.niniyumi.personalagent.auth.application.InvalidRefreshTokenException;
import com.niniyumi.personalagent.auth.application.InvalidPasswordResetCodeException;
import com.niniyumi.personalagent.auth.application.InvalidRegistrationVerificationCodeException;
import com.niniyumi.personalagent.auth.application.UsernameAlreadyExistsException;
import com.niniyumi.personalagent.weeklyreport.application.InvalidWeekStartException;
import com.niniyumi.personalagent.weeklyreport.application.InvalidDocxException;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportAlreadyExistsException;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportNotFoundException;
import com.niniyumi.personalagent.weeklyreport.application.NoWeeklyReportsForPeriodException;
import com.niniyumi.personalagent.weeklyreport.application.SummaryGenerationException;
import com.niniyumi.personalagent.weeklyreport.application.WorkSummaryNotFoundException;
import com.niniyumi.personalagent.weeklyreport.application.InvalidSummaryPeriodException;
import com.niniyumi.personalagent.course.application.CourseNotFoundException;
import com.niniyumi.personalagent.course.application.InvalidCoursePartsException;
import com.niniyumi.personalagent.course.application.InvalidCourseStateException;
import com.niniyumi.personalagent.course.application.InvalidCourseTitleException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(InvalidPasswordResetCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPasswordResetCode() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_RESET_CODE", "Invalid or expired verification code");
    }

    @ExceptionHandler(InvalidRegistrationVerificationCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRegistrationVerificationCode() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_CODE", "Invalid or expired verification code");
    }

    @ExceptionHandler(WeeklyReportNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWeeklyReportNotFound() {
        return error(HttpStatus.NOT_FOUND, "WEEKLY_REPORT_NOT_FOUND", "Weekly report not found");
    }

    @ExceptionHandler(WeeklyReportAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleWeeklyReportAlreadyExists() {
        return error(HttpStatus.CONFLICT, "WEEKLY_REPORT_EXISTS", "Weekly report already exists");
    }

    @ExceptionHandler(InvalidWeekStartException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWeekStart() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_WEEK_START", "Week must start on Monday");
    }

    @ExceptionHandler(InvalidDocxException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDocx() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_DOCX", "Invalid DOCX file");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleOversizedUpload() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_DOCX", "Invalid DOCX file");
    }

    @ExceptionHandler(NoWeeklyReportsForPeriodException.class)
    public ResponseEntity<ApiErrorResponse> handleNoWeeklyReportsForPeriod() {
        return error(HttpStatus.CONFLICT, "NO_WEEKLY_REPORTS_FOR_PERIOD", "No weekly reports for period");
    }

    @ExceptionHandler(WorkSummaryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkSummaryNotFound() {
        return error(HttpStatus.NOT_FOUND, "WORK_SUMMARY_NOT_FOUND", "Work summary not found");
    }

    @ExceptionHandler(SummaryGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleSummaryGenerationFailure() {
        return error(HttpStatus.BAD_GATEWAY, "SUMMARY_GENERATION_FAILED", "Summary generation failed");
    }

    @ExceptionHandler(InvalidSummaryPeriodException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSummaryPeriod() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCourseNotFound() {
        return error(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Course not found");
    }

    @ExceptionHandler({InvalidCoursePartsException.class, InvalidCourseTitleException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidCourseInput() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed");
    }

    @ExceptionHandler(InvalidCourseStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCourseState() {
        return error(HttpStatus.CONFLICT, "INVALID_COURSE_STATE", "Invalid course state");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled request exception, traceId={}", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "Unexpected server error", traceId));
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, UUID.randomUUID().toString()));
    }
}
