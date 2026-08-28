package com.urlshortener.url.controller;

import com.urlshortener.url.dto.CreateUrlRequest;
import com.urlshortener.url.dto.UpdateUrlRequest;
import com.urlshortener.url.dto.UrlResponse;
import com.urlshortener.url.service.UrlManagementService;
import com.urlshortener.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlManagementService urlManagementService;

    @PostMapping
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(urlManagementService.createShortUrl(request, user));
    }

    @PostMapping("/guest")
    public ResponseEntity<UrlResponse> createGuestUrl(
            @Valid @RequestBody CreateUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(urlManagementService.createGuestShortUrl(request));
    }

    @GetMapping
    public ResponseEntity<Page<UrlResponse>> getMyUrls(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(urlManagementService.getUserUrlsPaginated(user.getId(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UrlResponse> getUrlById(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(urlManagementService.getUrlById(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UrlResponse> updateUrl(
            @PathVariable String id,
            @Valid @RequestBody UpdateUrlRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(urlManagementService.updateUrl(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        urlManagementService.softDeleteUrl(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
