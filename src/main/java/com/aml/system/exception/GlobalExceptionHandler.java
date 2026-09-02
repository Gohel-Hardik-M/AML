package com.aml.system.exception;

import com.aml.system.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // <-- Added this import
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler intercepting all controller-layer errors.
 * Standardizes API responses and prevents raw stack trace leakage to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AmlBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleAmlBusinessException(AmlBusinessException ex, HttpServletRequest request) {

        // 1. SAFE NULL CHECKS: Handle exceptions thrown without an Enum
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode().getCode() : "AML_BUSINESS_ERROR";
        int statusCode = ex.getErrorCode() != null ? ex.getErrorCode().getHttpStatus() : HttpStatus.BAD_REQUEST.value();

        log.warn("AML Business Exception occurred on [{} {}] - Code: {}, Message: {}",
                request.getMethod(), request.getRequestURI(), errorCode, ex.getMessage());

        // Safe null check for context map
        Map<String, Object> metadata = new HashMap<>(ex.getErrorContext() != null ? ex.getErrorContext() : Map.of());
        metadata.put("path", request.getRequestURI());

        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        ApiResponse<Void> response = ApiResponse.error(
                errorCode,
                ex.getMessage(),
                metadata
        );

        return ResponseEntity.status(status).body(response);
    }

    // 2. NEW HANDLER: Catches @PreAuthorize role failures (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access Denied on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        Map<String, Object> metadata = Map.of("path", request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                "AML_ACCESS_DENIED",
                "Access denied: You do not have permission to access this resource.",
                metadata
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fieldErrors", fieldErrors);
        metadata.put("path", request.getRequestURI());

        ApiResponse<Void> response = ApiResponse.error(
                "AML_VALIDATION_ERROR",
                "Input payload validation failed",
                metadata
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled system exception caught on [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> metadata = Map.of(
                "path", request.getRequestURI(),
                "errorType", ex.getClass().getSimpleName()
        );

        // Fallback for missing enum, prevents compilation error if Enum lacks INTERNAL_SYSTEM_ERROR
        String code = "AML_5001";
        try {
            code = ErrorCodeEnum.INTERNAL_SYSTEM_ERROR.getCode();
        } catch (Exception ignored) {}

        ApiResponse<Void> response = ApiResponse.error(
                code,
                "An unexpected internal error occurred. Please contact compliance platform support.",
                metadata
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}