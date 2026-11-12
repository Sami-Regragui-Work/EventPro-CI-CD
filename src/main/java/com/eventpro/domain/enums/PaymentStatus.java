package com.eventpro.domain.enums;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public boolean canTransitionTo(PaymentStatus desiredStatus) {
        return switch (this) {
            case PENDING -> desiredStatus == SUCCESS || desiredStatus == FAILED;
            case SUCCESS, FAILED -> false;
        };
    }
}
