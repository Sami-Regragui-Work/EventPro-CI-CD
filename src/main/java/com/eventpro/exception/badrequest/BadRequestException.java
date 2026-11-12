package com.eventpro.exception.badrequest;

import com.eventpro.exception.BaseException;

public abstract class BadRequestException extends BaseException {
    protected BadRequestException(String message) {
        super(message);
    }

    protected BadRequestException() {
        this("Invalid request");
    }
}
