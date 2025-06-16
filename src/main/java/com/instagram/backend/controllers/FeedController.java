package com.instagram.backend.controllers;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;
import com.instagram.backend.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Feed APIs")
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class FeedController {

    private final FeedService feedService;

    @Operation(
            summary = "Get user feed",
            description = "Returns a paginated list of posts for the authenticated user. Supports infinite scroll using lastFetched timestamp."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getFeed(
            @RequestParam(required = false) Instant lastFetched,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching feed for current user. lastFetched={}, size={}", lastFetched, size);
        return ResponseEntity.ok(feedService.getUserFeed(lastFetched, size));
    }
}
