package com.instagram.backend.dtos.response;

import com.instagram.backend.entity.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String senderUsername;
    private String message;
    private NotificationType type;
    private LocalDateTime createdAt;
    private boolean isRead;
}
