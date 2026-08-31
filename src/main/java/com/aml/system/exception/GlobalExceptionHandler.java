package com.aml.system.exception;

import com.aml.system.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        log.warn("AML Business Exception occurred on [{} {}] - Code: {}, Message: {}",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode(), ex.getMessage());

        Map<String, Object> metadata = new HashMap<>(ex.getErrorContext());
        metadata.put("path", request.getRequestURI());

        HttpStatus status = HttpStatus.resolve(ex.getErrorCode().getHttpStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ApiResponse<Void> response = ApiResponse.error(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                metadata
        );

        return ResponseEntity.status(status).body(response);
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

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCodeEnum.INTERNAL_SYSTEM_ERROR.getCode(),
                "An unexpected internal error occurred. Please contact compliance platform support.",
                metadata
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
