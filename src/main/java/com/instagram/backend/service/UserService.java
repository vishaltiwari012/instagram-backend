package com.instagram.backend.service;


import com.instagram.backend.dtos.request.ChangePasswordRequest;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.UserDto;
import com.instagram.backend.dtos.response.UserProfileDto;
import com.instagram.backend.dtos.response.UserSearchResponse;

public interface UserService {
    ApiResponse<String> changePassword(ChangePasswordRequest request);
    ApiResponse<UserProfileDto> getMyProfile();
    ApiResponse<UserProfileDto> getUserProfile(Long userId);
    ApiResponse<UserSearchResponse> searchUsersByUsername(String username);
    ApiResponse<String> togglePrivacy(boolean privateProfile);
    boolean isPrivateProfile(Long userId);
    ApiResponse<UserDto> getUserById(Long userId);
}
