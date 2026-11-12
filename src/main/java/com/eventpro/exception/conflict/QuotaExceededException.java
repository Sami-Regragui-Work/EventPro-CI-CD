package com.eventpro.exception.conflict;

public class QuotaExceededException extends ConflictException {
    public QuotaExceededException(String message) {
        super(message);
    }
    public QuotaExceededException() {
        this("This client has reached the ticket limit for this category");
    }
}
