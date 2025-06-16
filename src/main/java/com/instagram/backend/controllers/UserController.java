package com.instagram.backend.controllers;


import com.instagram.backend.dtos.request.ChangePasswordRequest;
import com.instagram.backend.dtos.request.ConnectionRequestDto;
import com.instagram.backend.dtos.request.ProfilePrivacyToggleRequest;
import com.instagram.backend.dtos.response.*;
import com.instagram.backend.service.UserConnectionService;
import com.instagram.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User APIs")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserConnectionService userConnectionService;

    // ============================
    // PROFILE & SETTINGS
    // ============================

    @Operation(summary = "Change password", description = "Allows the authenticated user to change their password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Received change password request for current user");
        return ResponseEntity.ok(userService.changePassword(request));
    }


    @Operation(summary = "Get my profile", description = "Returns the profile details of the currently authenticated user")
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMyProfile() {
        log.info("Fetching profile for current user");
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @Operation(summary = "Get user profile", description = "Returns profile details of the user with the given ID")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserProfile(@PathVariable Long userId) {
        log.info("Fetching profile of user with ID: {}", userId);
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @Operation(summary = "Search users by username", description = "Search for users using their username (case-insensitive)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<UserSearchResponse>> searchUsers(@RequestParam("username") String username) {
        log.info("Searching users with username: {}", username);
        return ResponseEntity.ok(userService.searchUsersByUsername(username));
    }

    @Operation(summary = "Toggle profile privacy", description = "Switch profile visibility between public and private")
    @PatchMapping("/toggle-privacy")
    public ResponseEntity<ApiResponse<String>> togglePrivacy(@RequestBody ProfilePrivacyToggleRequest request) {
        log.info("Toggling profile privacy to: {}", request.isPrivateProfile());
        return ResponseEntity.ok(userService.togglePrivacy(request.isPrivateProfile()));
    }

    @Operation(summary = "Check if profile is private", description = "Check whether a user's profile is private")
    @GetMapping("/{userId}/privacy")
    public ResponseEntity<Boolean> isPrivate(@PathVariable Long userId) {
        log.info("Checking privacy status for user with ID: {}", userId);
        return ResponseEntity.ok(userService.isPrivateProfile(userId));
    }

    @Operation(summary = "Get user by ID", description = "Returns user details by user ID")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId) {
        log.info("Fetching user with ID: {}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
    }


    // ============================================
    // CONNECTION (FOLLOW/UNFOLLOW/BLOCK/UNBLOCK)
    // ============================================

    @Operation(summary = "Send follow request", description = "Follow another user by their user ID")
    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<String>> followUser(@RequestBody @Valid ConnectionRequestDto dto) {
        log.info("Follow request sent to user ID: {}", dto.getToUserId());
        return ResponseEntity.ok(userConnectionService.followUser(dto));
    }

    @Operation(summary = "Follow back user", description = "Follow back a user who follows you")
    @PostMapping("/follow-back")
    public ResponseEntity<ApiResponse<String>> followBackUser(@RequestBody @Valid ConnectionRequestDto dto) {
        log.info("Follow-back request to user ID: {}", dto.getToUserId());
        return ResponseEntity.ok(userConnectionService.followBackUser(dto));
    }

    @Operation(summary = "Unfollow user", description = "Unfollow a user you currently follow")
    @PostMapping("/unfollow")
    public ResponseEntity<ApiResponse<String>> unfollowUser(@RequestBody @Valid ConnectionRequestDto dto) {
        log.info("Unfollow request for user ID: {}", dto.getToUserId());
        return ResponseEntity.ok(userConnectionService.unfollowUser(dto));
    }

    @Operation(summary = "Block user", description = "Block a user to prevent further interaction")
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<String>> blockUser(@RequestBody @Valid ConnectionRequestDto dto) {
        log.info("Block request for user ID: {}", dto.getToUserId());
        return ResponseEntity.ok(userConnectionService.blockUser(dto));
    }

    @Operation(summary = "Unblock user", description = "Unblock a previously blocked user")
    @PostMapping("/unblock")
    public ResponseEntity<ApiResponse<String>> unblockUser(@RequestBody @Valid ConnectionRequestDto dto) {
        log.info("Unblock request for user ID: {}", dto.getToUserId());
        return ResponseEntity.ok(userConnectionService.unblockUser(dto));
    }


    // ============================
    // CONNECTIONS (GET LIST)
    // ============================

    @Operation(summary = "Get followers", description = "Get list of users following the authenticated user")
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getFollowers() {
        log.info("Fetching followers for current user");
        return ResponseEntity.ok(userConnectionService.getFollowers());
    }

    @Operation(summary = "Get following", description = "Get list of users the authenticated user is following")
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getFollowing() {
        log.info("Fetching users followed by current user");
        return ResponseEntity.ok(userConnectionService.getFollowing());
    }

    @Operation(summary = "Get blocked users", description = "Get list of users blocked by the authenticated user")
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getBlockedUsers() {
        log.info("Fetching blocked users list");
        return ResponseEntity.ok(userConnectionService.getBlockedUsers());
    }


    // ============================
    // PUBLIC USER CONNECTION INFO
    // ============================

    @Operation(summary = "Get followers of user", description = "Get followers of a specific user by user ID")
    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getFollowersOfUser(@PathVariable Long userId) {
        log.info("Fetching followers of user with ID: {}", userId);
        return ResponseEntity.ok(userConnectionService.getFollowersOfUser(userId));
    }

    @Operation(summary = "Get following of user", description = "Get users followed by a specific user")
    @GetMapping("/users/{userId}/following")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getFollowingOfUser(@PathVariable Long userId) {
        log.info("Fetching following list of user with ID: {}", userId);
        return ResponseEntity.ok(userConnectionService.getFollowingOfUser(userId));
    }

    @Operation(summary = "Get mutual friends", description = "Get mutual connections between current user and another user")
    @GetMapping("/users/{otherUserId}/mutual-friends")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getMutualFriends(@PathVariable Long otherUserId) {
        log.info("Fetching mutual friends between current user and user ID: {}", otherUserId);
        return ResponseEntity.ok(userConnectionService.getMutualFriendsBetween(otherUserId));
    }

    @Operation(summary = "Check follow status", description = "Check if the authenticated user is following the given user")
    @GetMapping("/is-following/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(@PathVariable Long userId) {
        log.info("Checking follow status for user ID: {}", userId);
        return ResponseEntity.ok(userConnectionService.isFollowingUser(userId));
    }

}