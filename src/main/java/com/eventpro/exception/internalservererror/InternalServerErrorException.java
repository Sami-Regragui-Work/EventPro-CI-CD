package com.eventpro.exception.internalservererror;

import com.eventpro.exception.BaseException;

public abstract class InternalServerErrorException extends BaseException {
    protected InternalServerErrorException(String message) {
        super(message);
    }
    protected InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
    protected InternalServerErrorException() {
        this("Unexpected server error");
    }
}
