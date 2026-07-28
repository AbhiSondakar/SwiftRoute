package com.example.urlshortener.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "url_mappings")
public class UrlMapping {

    @Id
    private String id;

    private String originalUrl;

    @Indexed(unique = true)
    private String shortCode;

    private Instant createdAt;

    private long clickCount;

    // ─── No-arg constructor ────────────────────────────────────────
    public UrlMapping() {
        this.createdAt = Instant.now();
        this.clickCount = 0L;
    }

    // ─── All-arg constructor ───────────────────────────────────────
    public UrlMapping(String id, String originalUrl, String shortCode,
            Instant createdAt, long clickCount) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.createdAt = createdAt;
        this.clickCount = clickCount;
    }

    // ─── Getters ───────────────────────────────────────────────────
    public String getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    // ─── Setters ───────────────────────────────────────────────────
    public void setId(String id) {
        this.id = id;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }

    // ─── Builder ───────────────────────────────────────────────────
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String originalUrl;
        private String shortCode;
        private Instant createdAt = Instant.now();
        private long clickCount = 0L;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder clickCount(long clickCount) {
            this.clickCount = clickCount;
            return this;
        }

        public UrlMapping build() {
            return new UrlMapping(id, originalUrl, shortCode, createdAt, clickCount);
        }
    }

    @Override
    public String toString() {
        return "UrlMapping{id='" + id + "', shortCode='" + shortCode +
                "', originalUrl='" + originalUrl + "', clickCount=" + clickCount + '}';
    }
}
