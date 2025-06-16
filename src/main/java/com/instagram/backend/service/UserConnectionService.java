package com.instagram.backend.service;


import com.instagram.backend.dtos.request.ConnectionRequestDto;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.ConnectionResponseDto;

import java.util.List;

public interface UserConnectionService {

    ApiResponse<String> followUser(ConnectionRequestDto dto);

    ApiResponse<String> followBackUser(ConnectionRequestDto dto);

    ApiResponse<String> unfollowUser(ConnectionRequestDto dto);

    ApiResponse<String> blockUser(ConnectionRequestDto dto);

    ApiResponse<String> unblockUser(ConnectionRequestDto dto);

    ApiResponse<List<ConnectionResponseDto>> getFollowers();

    ApiResponse<List<ConnectionResponseDto>> getFollowing();

    ApiResponse<List<ConnectionResponseDto>> getBlockedUsers();

    ApiResponse<List<ConnectionResponseDto>> getFollowersOfUser(Long userId);

    ApiResponse<List<ConnectionResponseDto>> getFollowingOfUser(Long userId);

    ApiResponse<List<ConnectionResponseDto>> getMutualFriendsBetween(Long otherUserId);

    ApiResponse<Boolean> isFollowingUser(Long userId);
}

