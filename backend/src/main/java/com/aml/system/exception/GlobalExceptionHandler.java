package com.aml.system.exception;

import com.aml.system.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import com.aml.system.multitenancy.TenantContextHolder;

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
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = resolveRequestBodyMessage(ex);
        log.warn("Unreadable request body on [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message, "INVALID_REQUEST_BODY", request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "Required parameter '" + ex.getParameterName() + "' is missing.";
        log.warn("Missing parameter on [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String msg = "Invalid format for parameter '" + ex.getName() + "'. Expected type: " + requiredType;
        log.warn("Type mismatch on [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg, request.getRequestURI()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Max upload size exceeded on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ApiResponse.error("File size exceeds the allowed server limit.", request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed.";
        }

        log.warn("Validation failed on [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message, "VALIDATION_ERROR", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String message = resolveDataIntegrityMessage(cause);
        HttpStatus status = isUniqueViolation(cause) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

        log.warn("Data integrity violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(), cause.getMessage(), ex);

        return ResponseEntity.status(status).body(ApiResponse.error(message, request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied.", request.getRequestURI()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Database access failure on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(
                "The database service is temporarily unavailable. Please try again later.",
                "DATABASE_UNAVAILABLE",
                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("An unexpected internal error occurred.", request.getRequestURI())
        );
    }

    // --- MERGED LOGIC: Handles both Tenant/Admin rules AND Batch Checksum rules ---
    private String resolveDataIntegrityMessage(Throwable cause) {
        String msg = safeDatabaseMessage(cause.getMessage());
        if (msg == null) {
            return "Database constraint violation.";
        }

        String lower = msg.toLowerCase();

        // 1. Batch / CSV Upload Duplication Rules
        if (lower.contains("checksum") || lower.contains("uk_batches_tenant_checksum")) {
            return "Conflict: This exact file has already been uploaded.";
        }
        if (lower.contains("batch_date") || lower.contains("uk_batches_tenant_date")) {
            return "Conflict: A batch for this tenant and date already exists.";
        }
        if (lower.contains("transaction_id") || lower.contains("transactions_pkey")) {
            return "Conflict: One or more transaction IDs already exist.";
        }

        // 2. Tenant / Admin User Duplication Rules
        if (isUniqueViolation(cause)) {
            if (lower.contains("tenant_id")) {
                return "Tenant already exists in the registry.";
            }
            if (lower.contains("username") || lower.contains("idx_user_tenant_username")) {
                return "Admin username already exists for this tenant.";
            }
            if (lower.contains("email")) {
                return "Admin email already exists.";
            }
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

    private String resolveRequestBodyMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String field = jsonFieldPath(invalidFormatException);
            Class<?> targetType = invalidFormatException.getTargetType();
            if (BigDecimal.class.equals(targetType)) {
                return "Field '" + field + "' must be a valid decimal number.";
            }
            if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
                return "Field '" + field + "' must be a whole number.";
            }
            if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
                return "Field '" + field + "' must be true or false.";
            }
            return "Field '" + field + "' has an invalid value. Expected " + targetType.getSimpleName() + ".";
        }
        if (cause instanceof JsonMappingException mappingException) {
            String field = jsonFieldPath(mappingException);
            return "Field '" + field + "' has an invalid value or structure.";
        }
        return "Request body contains invalid JSON or has an unsupported value.";
    }

    private String jsonFieldPath(JsonMappingException exception) {
        String path = exception.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? reference.getFieldName()
                        : String.valueOf(reference.getIndex()))
                .filter(part -> !"null".equals(part))
                .collect(Collectors.joining("."));
        return path.isBlank() ? "request body" : path;
    }
}