package com.SwiftRoute.analytics.listener;

import com.SwiftRoute.analytics.entity.ClickEvent;
import com.SwiftRoute.analytics.event.AnalyticsEvent;
import com.SwiftRoute.analytics.repository.ClickEventRepository;
import com.SwiftRoute.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void handleClickEvent(AnalyticsEvent event) {
        try {
            ClickEvent clickEvent = ClickEvent.builder()
                    .urlId(event.getUrlId())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .referer(event.getReferer())
                    .build();

            clickEventRepository.save(clickEvent);

            // Atomically increment the denormalized counter in PostgreSQL.
            // The JPQL update avoids a read-modify-write race.
            urlRepository.incrementClickCount(event.getUrlId());

            log.debug("Analytics recorded for URL ID: {}", event.getUrlId());
        } catch (Exception e) {
            // Log and swallow — analytics must never break the redirect flow
            log.error("Failed to record analytics for URL ID {}: {}", event.getUrlId(), e.getMessage());
        }
    }
}
