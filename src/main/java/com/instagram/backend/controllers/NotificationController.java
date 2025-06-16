package com.instagram.backend.controllers;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.NotificationResponse;
import com.instagram.backend.websocket.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification APIs")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get all notifications", description = "Fetches all notifications for the logged-in user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll() {
        log.info("Fetching all notifications for current user");
        return ResponseEntity.ok(notificationService.getNotificationsForUser());
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications for the logged-in user as read")
    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        log.info("Marking all notifications as read");
        return ResponseEntity.ok(notificationService.markAllAsRead());
    }

    @Operation(summary = "Get unread notification count", description = "Returns the number of unread notifications for the logged-in user")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        log.info("Counting unread notifications for current user");
        return ResponseEntity.ok(notificationService.countUnreadNotifications());
    }
}