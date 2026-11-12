package com.eventpro.exception.notfound;

import com.eventpro.exception.BaseException;

public abstract class NotFoundException extends BaseException {
    protected NotFoundException(String message) {
        super(message);
    }

    protected NotFoundException() {
        this("Resource not found");
    }
}
