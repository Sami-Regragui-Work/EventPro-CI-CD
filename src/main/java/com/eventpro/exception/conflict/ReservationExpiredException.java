package com.eventpro.exception.conflict;

public class ReservationExpiredException extends ConflictException {
    public ReservationExpiredException(String message) {
        super(message);
    }
    public ReservationExpiredException() {
        this("This reservation expired");
    }
}
