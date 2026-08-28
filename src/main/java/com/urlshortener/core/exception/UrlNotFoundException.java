package com.urlshortener.core.exception;

public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String identifier) {
        super("URL not found for identifier: " + identifier);
    }
}
