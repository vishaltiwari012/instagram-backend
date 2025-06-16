package com.instagram.backend.controllers;

import com.instagram.backend.dtos.response.AdminUserListDto;
import com.instagram.backend.dtos.response.AdminUserListResponse;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.ConnectionResponseDto;
import com.instagram.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin APIs")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuthController {

    private final AdminService adminService;

    // ============================
    // CRUD Operations on Users
    // ============================


    @GetMapping
    @Operation(summary = "Get all registered users", description = "Returns a list of all registered users.")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> getAllRegisteredUsers() {
        log.info("Admin requested list of all users");
        return ResponseEntity.ok(adminService.getAllRegisteredUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Fetch a user's details using their unique ID.")
    public ResponseEntity<ApiResponse<AdminUserListDto>> getUserById(@PathVariable Long id) {
        log.info("Admin fetching details for userId={}", id);
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by their ID.")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        log.info("Admin deleting userId={}", id);
        return ResponseEntity.ok(adminService.deleteUser(id));
    }

    // ============================
    // User Account Status
    // ============================

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable user account", description = "Enables a deactivated user account.")
    public ResponseEntity<ApiResponse<String>> enableUser(@PathVariable Long id) {
        log.info("Admin enabling userId={}", id);
        return ResponseEntity.ok(adminService.enableUser(id));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable user account", description = "Disables an active user account.")
    public ResponseEntity<ApiResponse<String>> disableUser(@PathVariable Long id) {
        log.info("Admin disabling userId={}", id);
        return ResponseEntity.ok(adminService.disableUser(id));
    }

    // ============================
    // Role Management
    // ============================

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role", description = "Assigns a new role (e.g., USER, MODERATOR) to the user.")
    public ResponseEntity<ApiResponse<String>> changeUserRole(
            @PathVariable Long id,
            @RequestParam String roleName) {
        log.info("Admin changing role of userId={} to role={}", id, roleName);
        return ResponseEntity.ok(adminService.changeUserRole(id, roleName));
    }

    // ============================
    // Search and Insights
    // ============================

    @GetMapping("/search")
    @Operation(summary = "Search users by username/email", description = "Searches users by username or email (partial or full match).")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> searchUsers(@RequestParam String username) {
        log.info("Admin searching users with keyword '{}'", username);
        return ResponseEntity.ok(adminService.searchUsersByUsername(username));
    }

    @GetMapping("/connections")
    @Operation(summary = "Get all user connections", description = "Returns all user connections for admin overview.")
    public ResponseEntity<ApiResponse<List<ConnectionResponseDto>>> getAllConnections() {
        log.info("Admin requested user connection data");
        return ResponseEntity.ok(adminService.getAllConnectionsForAdmin());
    }
}