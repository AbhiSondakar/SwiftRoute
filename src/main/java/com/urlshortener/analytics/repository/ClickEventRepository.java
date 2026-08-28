package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.ClickEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickEventRepository extends MongoRepository<ClickEvent, String> {
}
