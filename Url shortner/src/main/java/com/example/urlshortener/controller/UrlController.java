package com.example.urlshortener.controller;

import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.service.UrlShorteningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Validated
@RestController
@CrossOrigin(origins = "*")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);

    private final UrlShorteningService service;

    public UrlController(UrlShorteningService service) {
        this.service = service;
    }

    // ──────────────────────────────────────────────────────────────
    // POST /api/shorten
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/api/shorten")
    public ResponseEntity<Map<String, String>> shorten(@RequestBody @Valid ShortenRequest request) {
        String shortUrl = service.shortenUrl(request.url());
        log.info("Shortened URL: {}", shortUrl);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("shortUrl", shortUrl));
    }

    // ──────────────────────────────────────────────────────────────
    // GET /{code} – Redirect or return JSON if Accept: application/json
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{code}")
    public ResponseEntity<?> redirect(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = service.resolveShortCode(code);

        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return ResponseEntity.ok(Map.of("originalUrl", originalUrl));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build(); // 302
    }

    // ──────────────────────────────────────────────────────────────
    // GET /api/stats/{code}
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/api/stats/{code}")
    public ResponseEntity<StatsResponse> stats(@PathVariable String code) {
        UrlMapping mapping = service.getStats(code);
        return ResponseEntity.ok(new StatsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt()
        ));
    }

    // ──────────────────────────────────────────────────────────────
    // DELETE /api/shorten/{code}
    // ──────────────────────────────────────────────────────────────
    @DeleteMapping("/api/shorten/{code}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String code) {
        service.deleteMapping(code);
        return ResponseEntity.ok(Map.of("message", "Short URL deleted successfully."));
    }

    // ──────────────────────────────────────────────────────────────
    // Records (Request / Response DTOs)
    // ──────────────────────────────────────────────────────────────
    public record ShortenRequest(@NotBlank(message = "URL must not be blank") String url) {}

    public record StatsResponse(
            String shortCode,
            String originalUrl,
            long clickCount,
            Instant createdAt
    ) {}
}
