package com.urlshortener.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Lightweight event published on the redirect read path.
 * Contains only the data needed for analytics — no JPA entities.
 */
@Getter
public class AnalyticsEvent extends ApplicationEvent {

    private final String urlId;
    private final String ipAddress;
    private final String userAgent;
    private final String referer;

    public AnalyticsEvent(Object source, String urlId, String ipAddress, String userAgent, String referer) {
        super(source);
        this.urlId = urlId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }
}
