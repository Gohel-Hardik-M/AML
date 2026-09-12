package com.aml.system.exception;

import org.springframework.http.HttpStatus;

public class TenantRoutingException extends AmlBusinessException {
    public TenantRoutingException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
