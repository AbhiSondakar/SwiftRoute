package com.SwiftRoute.url.repository;

import com.SwiftRoute.url.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, String> {

    Optional<Url> findByShortCode(String shortCode);

    List<Url> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);

    Page<Url> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<Url> findByIdAndUserId(String id, String userId);

    @Modifying
    @Query("update Url u set u.clickCount = u.clickCount + 1 where u.id = :urlId")
    void incrementClickCount(@org.springframework.data.repository.query.Param("urlId") String urlId);
}
