package com.urlshortener.url.dto;

import java.time.LocalDateTime;

public record CachedUrl(
        String originalUrl,
        boolean isDeleted,
        LocalDateTime expiresAt,
        boolean hasPassword
) {}
