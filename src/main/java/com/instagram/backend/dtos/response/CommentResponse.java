package com.instagram.backend.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String username;
    private LocalDateTime commentedAt;
    private List<CommentResponse> replies;
    private int likeCount;
    private boolean likedByCurrentUser;
}

