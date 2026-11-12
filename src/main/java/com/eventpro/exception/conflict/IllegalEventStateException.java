package com.eventpro.exception.conflict;

public class IllegalEventStateException extends ConflictException {
    public IllegalEventStateException(String message) {
        super(message);
    }
    public IllegalEventStateException() {
        this("Cannot make this status transition");
    }
}
