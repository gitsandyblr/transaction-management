package com.tabcorp.transaction.management.exception;

public class ResourceNotFoundException extends RuntimeException {

    // Constructor with a custom message
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Optional: Constructor with a custom message and a cause
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}