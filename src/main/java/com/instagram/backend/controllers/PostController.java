package com.instagram.backend.controllers;

import com.instagram.backend.dtos.request.CommentRequest;
import com.instagram.backend.dtos.request.PostRequestDto;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.CommentResponse;
import com.instagram.backend.dtos.response.PostResponseDto;
import com.instagram.backend.service.PostActivityService;
import com.instagram.backend.service.PostService;
import com.instagram.backend.service.SavedPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Post APIs")
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class PostController {

    private final PostService postService;
    private final PostActivityService postActivityService;
    private final SavedPostService savedPostService;


    // ============================
    // POST CRUD
    // ============================

    @Operation(summary = "Create a new post", description = "Allows the logged-in user to create a new post")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(@ModelAttribute PostRequestDto postRequest) {
        log.info("Creating new post");
        return ResponseEntity.ok(postService.createPost(postRequest));
    }

    @Operation(summary = "Update a post", description = "Update an existing post by post ID")
    @PutMapping("/update/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @PathVariable Long postId,
            @ModelAttribute PostRequestDto postRequest) {
        log.info("Updating post ID: {}", postId);
        return ResponseEntity.ok(postService.updatePost(postId, postRequest));
    }

    @Operation(summary = "Delete a post", description = "Delete a post by its ID")
    @DeleteMapping("/delete/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(@PathVariable Long postId) {
        log.info("Deleting post ID: {}", postId);
        return ResponseEntity.ok(postService.deletePost(postId));
    }

    @Operation(summary = "Get a post by ID", description = "Retrieve a single post by its ID")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPostById(@PathVariable Long postId) {
        log.info("Fetching post ID: {}", postId);
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @Operation(summary = "Get all posts", description = "Retrieve all posts from all users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getAllPosts() {
        log.info("Fetching all posts");
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @Operation(summary = "Get my posts", description = "Get all posts created by the current user")
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getPostsOfLoggedInUser() {
        log.info("Fetching current user's posts");
        return ResponseEntity.ok(postService.getPostsOfLoggedInUser());
    }

    @Operation(summary = "Get posts by user ID", description = "Get all posts created by a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getPostsByUser(@PathVariable Long userId) {
        log.info("Fetching posts by user ID: {}", userId);
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    @Operation(summary = "Get paginated posts by user", description = "Get paginated posts by user ID")
    @GetMapping("/user/{userId}/page")
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPostsByUserPage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(postService.getPostsByUser(userId, pageable));
    }

    @Operation(summary = "Get mentioned user posts", description = "Get posts where a user is mentioned")
    @GetMapping("/mentions")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getMentionedPosts(@RequestParam Long userId) {
        return ResponseEntity.ok(postService.getMentionedPosts(userId));
    }

    @Operation(summary = "Get posts by hashtag", description = "Get posts that contain a specific hashtag")
    @GetMapping("/hashtag")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getPostsByHashtag(@RequestParam String tag) {
        return ResponseEntity.ok(postService.getPostsByHashtag(tag.toLowerCase()));
    }


    // ============================
    // POST INTERACTIONS
    // ============================

    @Operation(summary = "Like a post", description = "Like a post by its ID")
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<String>> likePost(@PathVariable Long postId) {
        return ResponseEntity.ok(postActivityService.likePost(postId));
    }

    @Operation(summary = "Unlike a post", description = "Unlike a post by its ID")
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<String>> unlikePost(@PathVariable Long postId) {
        return ResponseEntity.ok(postActivityService.unlikePost(postId));
    }

    @Operation(summary = "Comment on a post", description = "Add a comment to a post")
    @PostMapping("/{postId}/comment")
    public ResponseEntity<ApiResponse<CommentResponse>> commentPost(
            @PathVariable Long postId, @RequestBody CommentRequest request) {
        return ResponseEntity.ok(postActivityService.addComment(postId, request));
    }

    @Operation(summary = "Get all comments on a post", description = "Get list of comments for a post")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(postActivityService.getCommentsForPost(postId));
    }

    @Operation(summary = "Like a comment", description = "Like a comment by its ID")
    @PostMapping("/comment/{commentId}/like")
    public ResponseEntity<ApiResponse<String>> likeComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(postActivityService.likeComment(commentId));
    }

    @Operation(summary = "Unlike a comment", description = "Unlike a comment by its ID")
    @DeleteMapping("/comment/{commentId}/unlike")
    public ResponseEntity<ApiResponse<String>> unlikeComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(postActivityService.unlikeComment(commentId));
    }


    // ============================
    // SAVED POSTS
    // ============================


    @Operation(summary = "Save a post", description = "Save a post to the user's saved list")
    @PostMapping("/{postId}/save")
    public ResponseEntity<ApiResponse<String>> savePost(@PathVariable Long postId) {
        log.info("Saving post ID: {}", postId);
        return ResponseEntity.ok(savedPostService.savePost(postId));
    }

    @Operation(summary = "Unsave a post", description = "Remove a post from the user's saved list")
    @DeleteMapping("/{postId}/unsave")
    public ResponseEntity<ApiResponse<String>> unsavePost(@PathVariable Long postId) {
        log.info("Unsaving post ID: {}", postId);
        return ResponseEntity.ok(savedPostService.unSavePost(postId));
    }

    @Operation(summary = "Get saved posts", description = "Get all posts saved by the current user")
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getSavedPosts() {
        log.info("Fetching saved posts");
        return ResponseEntity.ok(savedPostService.getSavedPosts());
    }
}