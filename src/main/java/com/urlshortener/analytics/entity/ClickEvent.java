package com.urlshortener.analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "click_events")
@CompoundIndex(name = "idx_urlId_clickedAt", def = "{'urlId': 1, 'clickedAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    private String id;

    @Indexed
    private String urlId;

    private String ipAddress;

    private String userAgent;

    private String referer;

    @CreatedDate
    private LocalDateTime clickedAt;
}
