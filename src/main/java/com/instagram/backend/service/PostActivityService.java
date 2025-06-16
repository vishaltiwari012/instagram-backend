package com.instagram.backend.service;

import com.instagram.backend.dtos.request.CommentRequest;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.CommentResponse;

import java.util.List;

public interface PostActivityService {
    ApiResponse<String> likePost(Long postId);
    ApiResponse<String> unlikePost(Long postId);
    ApiResponse<CommentResponse> addComment(Long postId, CommentRequest request);
    ApiResponse<List<CommentResponse>> getCommentsForPost(Long postId);
    ApiResponse<String> likeComment(Long commentId);
    ApiResponse<String> unlikeComment(Long commentId);
}
