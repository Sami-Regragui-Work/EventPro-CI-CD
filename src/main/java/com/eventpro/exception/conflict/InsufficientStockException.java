package com.eventpro.exception.conflict;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message);
    }
    public InsufficientStockException() {
        this("Event slots are full");
    }
}
