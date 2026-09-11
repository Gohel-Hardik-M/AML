package com.aml.system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

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