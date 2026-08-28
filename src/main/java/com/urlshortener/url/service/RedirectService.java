package com.urlshortener.url.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.urlshortener.analytics.event.AnalyticsEvent;
import com.urlshortener.core.exception.PasswordRequiredException;
import com.urlshortener.core.exception.UrlGoneException;
import com.urlshortener.core.exception.UrlNotFoundException;
import com.urlshortener.url.dto.CachedUrl;
import com.urlshortener.url.entity.Url;
import com.urlshortener.url.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedirectService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.url-ttl-minutes}")
    private long redisTtlMinutes;

    private static final String REDIS_KEY_PREFIX = "url:";

    public RedirectService(UrlRepository urlRepository, StringRedisTemplate redisTemplate,
                           ApplicationEventPublisher eventPublisher, PasswordEncoder passwordEncoder) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public String resolveAndRedirect(String shortCode, String ipAddress, String userAgent, String referer) {
        String cachedValue = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + shortCode);

        if (cachedValue != null) {
            try {
                // Parse the cached JSON object
                CachedUrl cachedUrl = objectMapper.readValue(cachedValue, CachedUrl.class);
                
                // Validate actively even on a cache hit
                if (cachedUrl.isDeleted() || (cachedUrl.expiresAt() != null && cachedUrl.expiresAt().isBefore(LocalDateTime.now()))) {
                    throw new UrlGoneException(shortCode);
                }
                
                if (cachedUrl.hasPassword()) {
                    throw new PasswordRequiredException(shortCode);
                }

                publishEvent(shortCode, ipAddress, userAgent, referer);
                return cachedUrl.originalUrl();
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached URL for shortCode {}: {}", shortCode, e.getMessage());
                // Fall back to DB on corrupted cache
            }
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (url.isDeleted() || isExpired(url)) {
            // Cache the deleted/expired state too so future lookups fail fast without hitting DB
            cacheUrl(shortCode, url);
            throw new UrlGoneException(shortCode);
        }

        cacheUrl(shortCode, url);

        if (url.getPasswordHash() != null) {
            throw new PasswordRequiredException(shortCode);
        }

        publishEvent(shortCode, ipAddress, userAgent, referer, url.getId());

        return url.getOriginalUrl();
    }

    public String unlockUrl(String shortCode, String password) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (url.isDeleted() || isExpired(url)) {
            throw new UrlGoneException(shortCode);
        }

        if (url.getPasswordHash() == null) {
            return url.getOriginalUrl();
        }

        if (!passwordEncoder.matches(password, url.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password.");
        }

        return url.getOriginalUrl();
    }

    private boolean isExpired(Url url) {
        return url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private void cacheUrl(String shortCode, Url url) {
        try {
            CachedUrl cachedUrl = new CachedUrl(
                    url.getOriginalUrl(),
                    url.isDeleted(),
                    url.getExpiresAt(),
                    url.getPasswordHash() != null
            );
            String json = objectMapper.writeValueAsString(cachedUrl);
            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + shortCode,
                    json,
                    redisTtlMinutes,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize CachedUrl for Redis cache", e);
        }
    }

    private void publishEvent(String shortCode, String ipAddress, String userAgent, String referer) {
        urlRepository.findByShortCode(shortCode).ifPresent(url ->
                publishEvent(shortCode, ipAddress, userAgent, referer, url.getId())
        );
    }

    private void publishEvent(String shortCode, String ipAddress, String userAgent, String referer, String urlId) {
        AnalyticsEvent event = new AnalyticsEvent(this, urlId, ipAddress, userAgent, referer);
        eventPublisher.publishEvent(event);
        log.debug("Analytics event published for shortCode={}", shortCode);
    }
}
