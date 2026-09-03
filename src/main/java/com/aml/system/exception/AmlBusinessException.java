package com.aml.system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base custom unchecked exception for AML API flows.
 * Uses a simple HTTP status and message instead of exposing internal enum-based codes.
 */
@Getter
public class AmlBusinessException extends RuntimeException {

    private final HttpStatus status;

    public AmlBusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public AmlBusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public AmlBusinessException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
