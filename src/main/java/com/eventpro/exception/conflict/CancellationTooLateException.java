package com.eventpro.exception.conflict;

public class CancellationTooLateException extends ConflictException {
    public CancellationTooLateException(String message) {
        super(message);
    }
    public CancellationTooLateException() {
        this("This reservation can no longer be cancelled");
    }
}
