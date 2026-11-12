package com.eventpro.exception.badrequest;

import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class ValidationException extends MethodArgumentNotValidException {
    private final String message;

    public ValidationException(String message, BindingResult bindingResult) {
        super(ValidationException.placeholderParameter(), bindingResult);
        this.message = message;
    }

    public ValidationException(String message) {
        this(message, new BeanPropertyBindingResult(new Object(), "request"));
    }

    public ValidationException() {
        this("Validation failed", new BeanPropertyBindingResult(new Object(), "request"));
    }


    @Override
    public @NonNull String getMessage() {
        return message;
    }

    private static MethodParameter placeholderParameter() {
        try {
            return new MethodParameter(ValidationException.class.getDeclaredMethod("placeholder"), -1);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void placeholder() {}
}
