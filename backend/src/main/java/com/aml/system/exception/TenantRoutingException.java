package com.aml.system.exception;

public class TenantRoutingException extends RuntimeException {
    public TenantRoutingException(String message) {
        super(message);
    }

    public TenantRoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
