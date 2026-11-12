package com.eventpro.exception.conflict;

public class IllegalPaymentStateException extends ConflictException {
    public IllegalPaymentStateException(String message) {
        super(message);
    }
    public IllegalPaymentStateException() {
        this("Cannot make this payment status transition");
    }
}
