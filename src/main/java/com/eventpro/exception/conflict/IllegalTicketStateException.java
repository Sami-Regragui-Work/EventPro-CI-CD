package com.eventpro.exception.conflict;

public class IllegalTicketStateException extends ConflictException {
    public IllegalTicketStateException(String message) {
        super(message);
    }
    public IllegalTicketStateException() {
        this("Cannot make this ticket status transition");
    }
}
