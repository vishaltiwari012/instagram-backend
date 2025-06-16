package com.instagram.backend.dtos.request;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private Long parentId;
}

