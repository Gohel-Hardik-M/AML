package com.aml.system.exception;

public class NoActiveRulesException extends RuntimeException {
    public NoActiveRulesException(String message) {
        super(message);
    }

    public NoActiveRulesException(String message, Throwable cause) {
        super(message, cause);
    }
}
