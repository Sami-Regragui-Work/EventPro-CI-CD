package com.eventpro.exception.conflict;

public class IdempotencyConflictException extends ConflictException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
    public IdempotencyConflictException() {
        this("Repeated Idempotency key for a different cart");
    }
}
