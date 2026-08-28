package com.urlshortener.core.exception;

/**
 * Thrown when a password-protected link is accessed without providing credentials.
 * Maps to HTTP 401 Unauthorized.
 */
public class PasswordRequiredException extends RuntimeException {

    private final String shortCode;

    public PasswordRequiredException(String shortCode) {
        super("This link is password-protected. Please provide the password to unlock.");
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
