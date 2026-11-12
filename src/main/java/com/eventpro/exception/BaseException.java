package com.eventpro.exception;

public abstract class BaseException extends RuntimeException {
    protected BaseException(String message) {
        super(message);
    }
    protected BaseException(String message, Throwable cause) {
        super(message, cause);
    }
    protected BaseException() {
        this("Could not recognize the exception");
    }
}
