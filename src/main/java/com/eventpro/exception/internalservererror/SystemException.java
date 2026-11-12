package com.eventpro.exception.internalservererror;

public class SystemException extends InternalServerErrorException {
    public SystemException(String message) {
        super(message);
    }
    public SystemException() {
        super();
    }
    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
