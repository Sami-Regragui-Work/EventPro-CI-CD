package com.eventpro.exception.conflict;

public class PromotionInUseException extends ConflictException {
    public PromotionInUseException(String message) {
        super(message);
    }
    public PromotionInUseException() {
        this("This promotion has been used and cannot be deleted");
    }
}
