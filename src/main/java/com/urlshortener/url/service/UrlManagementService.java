package com.urlshortener.url.service;

import com.urlshortener.core.exception.UrlNotFoundException;
import com.urlshortener.url.dto.CreateUrlRequest;
import com.urlshortener.url.dto.UpdateUrlRequest;
import com.urlshortener.url.dto.UrlResponse;
import com.urlshortener.url.entity.Url;
import com.urlshortener.url.repository.UrlRepository;
import com.urlshortener.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlManagementService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.redis.url-ttl-minutes}")
    private long redisTtlMinutes;

    private static final String REDIS_KEY_PREFIX = "url:";
    private static final int MAX_RETRIES = 5;

    public UrlResponse createShortUrl(CreateUrlRequest request, User user) {
        validateOriginalUrl(request.originalUrl());

        for (int i = 0; i < MAX_RETRIES; i++) {
            String shortCode = generateRandomShortCode();
            String redisKey = REDIS_KEY_PREFIX + shortCode;

            String cachedUrlJson = String.format(
                "{\"originalUrl\":\"%s\",\"isDeleted\":false,\"expiresAt\":%s,\"hasPassword\":%b}",
                request.originalUrl().replace("\"", "\\\""),
                request.expiresAt() == null ? "null" : "\"" + request.expiresAt().toString() + "\"",
                request.password() != null && !request.password().isBlank()
            );

            // Step 1: Redis SETNX as the primary shared reservation mechanism across all links
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    redisKey,
                    cachedUrlJson,
                    redisTtlMinutes,
                    TimeUnit.MINUTES
            );

            if (Boolean.TRUE.equals(success)) {
                Url url = Url.builder()
                        .originalUrl(request.originalUrl())
                        .shortCode(shortCode)
                        .userId(user.getId())
                        .expiresAt(request.expiresAt())
                        .build();

                if (request.password() != null && !request.password().isBlank()) {
                    url.setPasswordHash(passwordEncoder.encode(request.password()));
                }

                try {
                    // Step 2: Mongo Unique Index as backstop
                    url = urlRepository.save(url);
                    log.info("Created short URL: {} -> {} (attempt {})", shortCode, request.originalUrl(), i + 1);
                    return toResponse(url);
                } catch (DuplicateKeyException e) {
                    // Rollback Redis reservation if Mongo rejects it
                    redisTemplate.delete(redisKey);
                    log.warn("Mongo collision detected for shortCode: {}. Retrying...", shortCode);
                }
            } else {
                log.warn("Redis collision detected for shortCode: {}. Retrying...", shortCode);
            }
        }
        
        log.error("CRITICAL: Failed to generate a unique short code after {} attempts. This usually indicates a broken RNG or extreme concurrency collision rate, not an exhausted code space.", MAX_RETRIES);
        throw new IllegalStateException("Failed to generate a unique short code. Please try again.");
    }

    public UrlResponse createGuestShortUrl(CreateUrlRequest request) {
        validateOriginalUrl(request.originalUrl());

        for (int i = 0; i < MAX_RETRIES; i++) {
            String shortCode = generateRandomShortCode();
            String redisKey = REDIS_KEY_PREFIX + shortCode;
            
            String cachedUrlJson = String.format(
                "{\"originalUrl\":\"%s\",\"isDeleted\":false,\"expiresAt\":null,\"hasPassword\":false}",
                request.originalUrl().replace("\"", "\\\"")
            );

            // Step 1: Redis SETNX as the primary shared reservation mechanism
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    redisKey,
                    cachedUrlJson,
                    24,
                    TimeUnit.HOURS
            );

            if (Boolean.TRUE.equals(success)) {
                log.info("Created guest short URL: {} -> {} (attempt {})", shortCode, request.originalUrl(), i + 1);
                return new UrlResponse(
                        null,
                        shortCode,
                        baseUrl + "/" + shortCode,
                        request.originalUrl(),
                        false,
                        LocalDateTime.now().plusHours(24),
                        0,
                        LocalDateTime.now()
                );
            }
            log.warn("Redis collision detected for guest shortCode: {}. Retrying...", shortCode);
        }
        
        log.error("CRITICAL: Failed to generate a unique guest short code after {} attempts. This usually indicates a broken RNG or extreme concurrency collision rate.", MAX_RETRIES);
        throw new IllegalStateException("Failed to generate a unique guest short code. Please try again.");
    }

    private String generateRandomShortCode() {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(characters.charAt(java.util.concurrent.ThreadLocalRandom.current().nextInt(characters.length())));
        }
        return sb.toString();
    }

    private void validateOriginalUrl(String originalUrl) {
        if (originalUrl != null && originalUrl.startsWith(baseUrl)) {
            throw new IllegalArgumentException("Cannot shorten a URL that points to this service (redirect loop)");
        }
    }

    public List<UrlResponse> getUserUrls(String userId) {
        return urlRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public org.springframework.data.domain.Page<UrlResponse> getUserUrlsPaginated(String userId, int page, int size) {
        return urlRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
                userId,
                org.springframework.data.domain.PageRequest.of(page, size)
        ).map(this::toResponse);
    }

    public UrlResponse getUrlById(String urlId, String userId) {
        Url url = urlRepository.findByIdAndUserId(urlId, userId)
                .orElseThrow(() -> new UrlNotFoundException(urlId));
        return toResponse(url);
    }

    public UrlResponse updateUrl(String urlId, UpdateUrlRequest request, String userId) {
        Url url = urlRepository.findByIdAndUserId(urlId, userId)
                .orElseThrow(() -> new UrlNotFoundException(urlId));

        if (request.originalUrl() != null && !request.originalUrl().isBlank()) {
            validateOriginalUrl(request.originalUrl());
            url.setOriginalUrl(request.originalUrl());
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                url.setPasswordHash(null);
            } else {
                url.setPasswordHash(passwordEncoder.encode(request.password()));
            }
        }

        if (request.expiresAt() != null) {
            url.setExpiresAt(request.expiresAt());
        }

        url = urlRepository.save(url);
        evictCache(url.getShortCode());
        log.info("Updated URL id={}, cache evicted for shortCode={}", urlId, url.getShortCode());

        return toResponse(url);
    }

    public void softDeleteUrl(String urlId, String userId) {
        Url url = urlRepository.findByIdAndUserId(urlId, userId)
                .orElseThrow(() -> new UrlNotFoundException(urlId));

        url.setDeleted(true);
        urlRepository.save(url);

        evictCache(url.getShortCode());
        log.info("Soft-deleted URL id={}, cache evicted for shortCode={}", urlId, url.getShortCode());
    }

    private void evictCache(String shortCode) {
        String key = REDIS_KEY_PREFIX + shortCode;
        redisTemplate.delete(key);
    }

    private UrlResponse toResponse(Url url) {
        return new UrlResponse(
                url.getId(),
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getOriginalUrl(),
                url.getPasswordHash() != null,
                url.getExpiresAt(),
                url.getClickCount(),
                url.getCreatedAt()
        );
    }
}
