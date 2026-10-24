package com.SwiftRoute.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String name
) {}
