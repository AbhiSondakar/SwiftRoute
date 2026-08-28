package com.urlshortener.url.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UrlResponse(
        String id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        boolean passwordProtected,
        LocalDateTime expiresAt,
        long clickCount,
        LocalDateTime createdAt
) {}
