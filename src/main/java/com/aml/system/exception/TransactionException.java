package com.aml.system.exception;

public class TransactionException extends RuntimeException {
    public TransactionException(String message){
        super(message);
    }
}
