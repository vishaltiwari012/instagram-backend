package com.instagram.backend.service;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;

import java.time.Instant;
import java.util.List;

public interface FeedService {
    ApiResponse<List<PostResponseDto>> getUserFeed(Instant lastFetched, int size);
}
