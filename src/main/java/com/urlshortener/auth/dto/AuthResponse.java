package com.urlshortener.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String name
) {}
