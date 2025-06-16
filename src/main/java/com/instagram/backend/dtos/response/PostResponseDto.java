package com.instagram.backend.dtos.response;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class PostResponseDto {
    private Long id;
    private String caption;
    private String imageUrl;
    private String username;
    private Instant createdAt;
    private int likeCount;
    private int commentCount;
    private List<String> hashtags;
    private List<String> mentions;
}
