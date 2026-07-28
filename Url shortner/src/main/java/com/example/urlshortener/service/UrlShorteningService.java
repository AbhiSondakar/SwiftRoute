package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;

@Service
public class UrlShorteningService {

    private static final Logger log = LoggerFactory.getLogger(UrlShorteningService.class);
    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final UrlMappingRepository repository;
    private final MongoTemplate mongoTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    public UrlShorteningService(UrlMappingRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Shortens the given URL. If the same URL was already shortened,
     * returns the existing mapping.
     */
    public String shortenUrl(String originalUrl) {
        validateUrl(originalUrl);

        // Return existing short code if URL already exists
        Optional<UrlMapping> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            log.debug("URL already exists, returning existing short code: {}", existing.get().getShortCode());
            return buildShortUrl(existing.get().getShortCode());
        }

        // Generate a unique short code with collision handling
        String shortCode = generateUniqueCode();
        UrlMapping mapping = UrlMapping.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        repository.save(mapping);
        log.info("Created short code '{}' for URL: {}", shortCode, originalUrl);
        return buildShortUrl(shortCode);
    }

    /**
     * Resolves a short code to its original URL and increments the click count.
     */
    public String resolveShortCode(String shortCode) {
        Query query = Query.query(Criteria.where("shortCode").is(shortCode));
        Update update = new Update().inc("clickCount", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        UrlMapping mapping = mongoTemplate.findAndModify(query, update, options, UrlMapping.class);

        if (mapping == null) {
            log.warn("Short code not found: {}", shortCode);
            throw new ShortCodeNotFoundException(shortCode);
        }

        log.debug("Resolved short code '{}' to '{}'. Total clicks: {}",
                shortCode, mapping.getOriginalUrl(), mapping.getClickCount());
        return mapping.getOriginalUrl();
    }

    /**
     * Retrieves statistics for a given short code without incrementing clicks.
     */
    public UrlMapping getStats(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    /**
     * Deletes a URL mapping by its short code.
     */
    public void deleteMapping(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
        repository.delete(mapping);
        log.info("Deleted mapping for short code: {}", shortCode);
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL must not be blank.");
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                throw new InvalidUrlException("URL must be an absolute HTTP/HTTPS URL: " + url);
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL format: " + url);
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            String code = Base62Encoder.generateRandomCode();
            if (!repository.existsByShortCode(code)) {
                return code;
            }
            log.debug("Short code collision detected, retrying... (attempt {})", attempt + 1);
        }
        throw new IllegalStateException(
                "Failed to generate a unique short code after " + MAX_RETRY_ATTEMPTS + " attempts.");
    }

    private String buildShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }
}
