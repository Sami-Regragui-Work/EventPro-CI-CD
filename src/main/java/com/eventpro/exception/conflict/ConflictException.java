package com.eventpro.exception.conflict;

import com.eventpro.exception.BaseException;

public abstract class ConflictException extends BaseException {
    protected ConflictException(String message) {
        super(message);
    }

    protected ConflictException() {
        this("Request conflicts with the current state");
    }
}
