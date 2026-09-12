package com.aml.system.exception;

import org.springframework.http.HttpStatus;

public class DuplicateTransactionException extends AmlBusinessException {
    public DuplicateTransactionException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
