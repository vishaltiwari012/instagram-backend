package com.instagram.backend.service;

import com.instagram.backend.dtos.response.AdminUserListDto;
import com.instagram.backend.dtos.response.AdminUserListResponse;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.ConnectionResponseDto;

import java.util.List;

public interface AdminService {
    ApiResponse<AdminUserListResponse> getAllRegisteredUsers();
    ApiResponse<AdminUserListDto> getUserById(Long id);
    ApiResponse<String> enableUser(Long id);
    ApiResponse<String> disableUser(Long id);
    ApiResponse<String> deleteUser(Long id);
    ApiResponse<String> changeUserRole(Long id, String roleName);
    ApiResponse<AdminUserListResponse> searchUsersByUsername(String username);
    ApiResponse<List<ConnectionResponseDto>> getAllConnectionsForAdmin();
}
