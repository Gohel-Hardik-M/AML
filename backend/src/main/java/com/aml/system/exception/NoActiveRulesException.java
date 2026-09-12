package com.aml.system.exception;

import org.springframework.http.HttpStatus;

public class NoActiveRulesException extends AmlBusinessException {
    public NoActiveRulesException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
