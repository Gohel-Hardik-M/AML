package com.aml.system.exception;

import com.aml.system.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Global exception handling with concise, client-friendly API errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AmlBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleAmlBusinessException(AmlBusinessException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;

        log.warn("Business exception on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(status).body(
                ApiResponse.error(ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation failed on [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(message, request.getRequestURI())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String message = resolveDataIntegrityMessage(cause);
        HttpStatus status = isUniqueViolation(cause) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

        log.warn("Data integrity violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(), cause.getMessage(), ex);

        return ResponseEntity.status(status).body(
                ApiResponse.error(message, request.getRequestURI())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error("Access denied.", request.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected internal error occurred.", request.getRequestURI())
        );
    }

    private String resolveDataIntegrityMessage(Throwable cause) {
        String msg = safeDatabaseMessage(cause.getMessage());
        if (msg == null) {
            return "Database constraint violation.";
        }

        String lower = msg.toLowerCase();

        if (isUniqueViolation(cause) && lower.contains("tenant_id")) {
            return "Tenant already exists.";
        }

        if (isUniqueViolation(cause) && (lower.contains("username") || lower.contains("idx_user_tenant_username"))) {
            return "Admin username already exists for this tenant.";
        }

        if (isUniqueViolation(cause) && lower.contains("email")) {
            return "Admin email already exists.";
        }

        return "Database constraint violation: " + msg;
    }

    private String safeDatabaseMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        int detailIndex = message.indexOf("<EOL> Detail:");
        if (detailIndex >= 0) {
            message = message.substring(0, detailIndex);
        }

        if (message.startsWith("ERROR: ")) {
            message = message.substring("ERROR: ".length());
        }

        return message.trim();
    }

    private boolean isUniqueViolation(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof SQLException && "23505".equals(((SQLException) current).getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}