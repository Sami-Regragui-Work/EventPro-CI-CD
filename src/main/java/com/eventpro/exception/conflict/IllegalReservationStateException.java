package com.eventpro.exception.conflict;

public class IllegalReservationStateException extends ConflictException {
    public IllegalReservationStateException(String message) {
        super(message);
    }
    public IllegalReservationStateException() {
        this("Cannot make this reservation status transition");
    }
}
