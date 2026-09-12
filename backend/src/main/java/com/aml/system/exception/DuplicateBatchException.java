package com.aml.system.exception;

import org.springframework.http.HttpStatus;

public class DuplicateBatchException extends AmlBusinessException {
    public DuplicateBatchException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
