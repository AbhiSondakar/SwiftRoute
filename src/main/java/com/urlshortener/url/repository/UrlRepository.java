package com.urlshortener.url.repository;

import com.urlshortener.url.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends MongoRepository<Url, String> {

    Optional<Url> findByShortCode(String shortCode);

    List<Url> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);

    Page<Url> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<Url> findByIdAndUserId(String id, String userId);

    @Query("{'_id': ?0}")
    @Update("{'$inc': {'clickCount': 1}}")
    void incrementClickCount(String urlId);
}
