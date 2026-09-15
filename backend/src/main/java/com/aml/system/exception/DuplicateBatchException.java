package com.aml.system.exception;

public class DuplicateBatchException extends RuntimeException {
    public DuplicateBatchException(String message) {
        super(message);
    }

    public DuplicateBatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
