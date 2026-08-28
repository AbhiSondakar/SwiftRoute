package com.urlshortener.url.dto;

import jakarta.validation.constraints.NotBlank;

public record UnlockUrlRequest(

        @NotBlank(message = "Password is required")
        String password
) {}
