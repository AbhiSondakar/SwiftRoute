package com.urlshortener.analytics.listener;

import com.urlshortener.analytics.entity.ClickEvent;
import com.urlshortener.analytics.event.AnalyticsEvent;
import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous listener that consumes {@link AnalyticsEvent}s
 * and persists them as {@link ClickEvent} records.
 *
 * <p>Runs on a separate thread pool so that the redirect read path
 * never blocks on database writes.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsListener {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    @Async("analyticsExecutor")
    @EventListener
    public void handleClickEvent(AnalyticsEvent event) {
        try {
            ClickEvent clickEvent = ClickEvent.builder()
                    .urlId(event.getUrlId())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .referer(event.getReferer())
                    .build();

            clickEventRepository.save(clickEvent);

            // Atomically increment the denormalized counter on the URL document.
            // Uses MongoDB $inc — no read-modify-write, no race conditions.
            urlRepository.incrementClickCount(event.getUrlId());

            log.debug("Analytics recorded for URL ID: {}", event.getUrlId());
        } catch (Exception e) {
            // Log and swallow — analytics must never break the redirect flow
            log.error("Failed to record analytics for URL ID {}: {}", event.getUrlId(), e.getMessage());
        }
    }
}
