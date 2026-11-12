package com.eventpro.exception.conflict;

public class TicketCategoryInUseException extends ConflictException {
    public TicketCategoryInUseException(String message) {
        super(message);
    }

    public TicketCategoryInUseException() {
        this("This ticketCategory has reservations and cannot be removed");
    }
}
