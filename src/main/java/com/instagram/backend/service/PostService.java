package com.instagram.backend.service;

import com.instagram.backend.dtos.request.PostRequestDto;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.PostResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {
    ApiResponse<PostResponseDto> createPost(PostRequestDto postRequest);
    ApiResponse<PostResponseDto> updatePost(Long postId, PostRequestDto postRequest);
    ApiResponse<String> deletePost(Long postId);
    ApiResponse<PostResponseDto> getPostById(Long postId);
    ApiResponse<List<PostResponseDto>> getAllPosts();
    ApiResponse<List<PostResponseDto>> getPostsOfLoggedInUser();
    ApiResponse<List<PostResponseDto>> getPostsByUser(Long userId);
    ApiResponse<Page<PostResponseDto>> getPostsByUser(Long userId, Pageable pageable);
    ApiResponse<List<PostResponseDto>> getMentionedPosts(Long userId);
    ApiResponse<List<PostResponseDto>> getPostsByHashtag(String tag);
}
