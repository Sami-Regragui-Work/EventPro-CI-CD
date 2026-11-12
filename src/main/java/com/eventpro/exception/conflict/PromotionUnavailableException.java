package com.eventpro.exception.conflict;

public class PromotionUnavailableException extends ConflictException {
    public PromotionUnavailableException(String message) {
        super(message);
    }
    public PromotionUnavailableException() {
        this("This promotion is not valid today or has been used up");
    }
}
