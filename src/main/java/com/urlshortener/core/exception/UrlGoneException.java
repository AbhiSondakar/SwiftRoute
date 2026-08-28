package com.urlshortener.core.exception;

/**
 * Thrown when a URL has been soft-deleted or has expired.
 * Maps to HTTP 410 Gone.
 */
public class UrlGoneException extends RuntimeException {

    public UrlGoneException(String shortCode) {
        super("The link '" + shortCode + "' is no longer available.");
    }
}
