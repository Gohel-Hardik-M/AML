package com.aml.system.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Base custom unchecked exception for the AML Platform.
 * Carries an ErrorCodeEnum, optional contextual details for auditing, and timestamp.
 */
@Getter
public class AmlBusinessException extends RuntimeException {

    private final ErrorCodeEnum errorCode;
    private final Map<String, Object> errorContext;

    public AmlBusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.errorContext = Map.of();
    }

    public AmlBusinessException(ErrorCodeEnum errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.errorContext = Map.of();
    }

    public AmlBusinessException(ErrorCodeEnum errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.errorContext = Map.of();
    }

    public AmlBusinessException(ErrorCodeEnum errorCode, String customMessage, Map<String, Object> errorContext) {
        super(customMessage);
        this.errorCode = errorCode;
        this.errorContext = errorContext != null ? errorContext : Map.of();
    }

    public AmlBusinessException(ErrorCodeEnum errorCode, String customMessage, Map<String, Object> errorContext, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.errorContext = errorContext != null ? errorContext : Map.of();
    }
}
