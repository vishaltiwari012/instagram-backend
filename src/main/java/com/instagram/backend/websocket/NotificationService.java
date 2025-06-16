package com.instagram.backend.websocket;


import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.NotificationResponse;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    void sendNotification(User fromUser, User toUser, NotificationType type);
    ApiResponse<List<NotificationResponse>> getNotificationsForUser();
    ApiResponse<String> markAllAsRead();
    ApiResponse<Long> countUnreadNotifications();
}
