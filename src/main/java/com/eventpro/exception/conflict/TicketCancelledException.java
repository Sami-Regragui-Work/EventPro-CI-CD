package com.eventpro.exception.conflict;

public class TicketCancelledException extends ConflictException {
    public TicketCancelledException(String message) {
        super(message);
    }
    public TicketCancelledException() {
        this("This ticket has been canceled");
    }
}
