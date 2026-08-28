package com.urlshortener.url.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

public record UpdateUrlRequest(

        @URL(message = "Must be a valid URL")
        @Pattern(regexp = "^https?://.*", message = "Only http and https URLs are allowed")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String originalUrl,

        String password,

        LocalDateTime expiresAt
) {}
