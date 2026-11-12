package com.eventpro.exception.conflict;

public class TicketAlreadyUsedException extends ConflictException {
    public TicketAlreadyUsedException(String message) {
        super(message);
    }
    public TicketAlreadyUsedException() {
        this("This ticket has been used already");
    }
}
