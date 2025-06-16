package com.instagram.backend.service;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;

import java.util.List;

public interface SavedPostService {
    ApiResponse<String> savePost(Long postId);
    ApiResponse<String> unSavePost(Long postId);
    ApiResponse<List<PostResponseDto>> getSavedPosts();
}
