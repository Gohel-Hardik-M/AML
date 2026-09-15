package com.aml.system.exception;

import com.aml.system.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Global exception handler providing a clean, consistent error flow.
 * Every exception is wrapped into ErrorResponseDto containing status code, error, message, and path.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized access [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDto> handleForbiddenException(
            ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden access [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleConflictException(
            ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict detected [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateBatchException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateBatchException(
            DuplicateBatchException ex, HttpServletRequest request) {
        log.warn("Duplicate batch on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateTransactionException(
            DuplicateTransactionException ex, HttpServletRequest request) {
        log.warn("Duplicate transaction on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(NoActiveRulesException.class)
    public ResponseEntity<ErrorResponseDto> handleNoActiveRulesException(
            NoActiveRulesException ex, HttpServletRequest request) {
        log.warn("No active rules on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TenantRoutingException.class)
    public ResponseEntity<ErrorResponseDto> handleTenantRoutingException(
            TenantRoutingException ex, HttpServletRequest request) {
        log.warn("Tenant routing exception on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ErrorResponseDto> handleTransactionException(
            TransactionException ex, HttpServletRequest request) {
        log.warn("Transaction error on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AmlBusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleAmlBusinessException(
            AmlBusinessException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        log.warn("Business exception on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(status, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = resolveRequestBodyMessage(ex);
        log.warn("Unreadable request body on [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed.";
        }

        log.warn("Validation failed on [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "Required parameter '" + ex.getParameterName() + "' is missing.";
        log.warn("Missing parameter on [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return buildResponse(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String msg = "Invalid format for parameter '" + ex.getName() + "'. Expected type: " + requiredType;
        log.warn("Type mismatch on [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return buildResponse(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Max upload size exceeded on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the allowed server limit.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause() : ex;
        String message = resolveDataIntegrityMessage(cause);
        HttpStatus status = isUniqueViolation(cause) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

        log.warn("Data integrity violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(), cause.getMessage());
        return buildResponse(status, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied.", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDto> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Database access failure on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "The database service is temporarily unavailable. Please try again later.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred.", request);
    }

    // --- Helper Methods ---

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request != null ? request.getRequestURI() : ""
        );
        return ResponseEntity.status(status).body(errorDto);
    }

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