package com.urlshortener.url.controller;

import com.urlshortener.url.dto.UnlockUrlRequest;
import com.urlshortener.url.dto.UnlockUrlResponse;
import com.urlshortener.url.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request
    ) {
        String originalUrl = redirectService.resolveAndRedirect(
                shortCode,
                extractIp(request),
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(HttpHeaders.REFERER)
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @PostMapping("/{shortCode}/unlock")
    public ResponseEntity<UnlockUrlResponse> unlock(
            @PathVariable String shortCode,
            @Valid @RequestBody UnlockUrlRequest request
    ) {
        String originalUrl = redirectService.unlockUrl(shortCode, request.password());
        return ResponseEntity.ok(new UnlockUrlResponse(originalUrl));
    }

    private String extractIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
