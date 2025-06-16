package com.instagram.backend.websocket;

import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.NotificationResponse;
import com.instagram.backend.entity.Notification;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.enums.NotificationType;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.NotificationRepository;
import com.instagram.backend.repository.UserRepository;
import com.instagram.backend.service.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineUserService onlineUserService;
    private final AuthenticationFacade authenticationFacade;
    private final UserRepository userRepository;

    @Override
    public void sendNotification(User fromUser, User toUser, NotificationType type) {
        log.info("Preparing to send '{}' notification from '{}' to '{}'", type, fromUser.getUsername(), toUser.getUsername());

        String message = createNotificationMessage(fromUser.getUsername(), type);

        Notification notification = Notification.builder()
                .sender(fromUser)
                .recipient(toUser)
                .message(message)
                .type(type)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved to DB: {}", notification);

        Long recipientId = toUser.getId();
        String recipientIdStr = recipientId.toString();

        if (onlineUserService.isUserOnline(recipientId)) {
            log.info("User '{}' is online. Sending WebSocket notification...", toUser.getUsername());

            try {
                messagingTemplate.convertAndSendToUser(
                        recipientIdStr,
                        "/queue/notifications",
                        toDto(notification)
                );
                log.info("WebSocket notification sent to user '{}'", toUser.getUsername());
            } catch (Exception ex) {
                log.error("Error sending WebSocket notification to user '{}': {}", toUser.getUsername(), ex.getMessage());
                // Optionally handle failure
            }
        } else {
            log.info("User '{}' is offline. Notification stored in DB for later retrieval.", toUser.getUsername());
        }
    }

    private String createNotificationMessage(String fromUsername, NotificationType type) {
        return switch (type) {
            case FOLLOW -> fromUsername + " followed you.";
            case UNFOLLOW -> fromUsername + " unfollowed you.";
            case BLOCK -> fromUsername + " blocked you.";
            case UNBLOCK -> fromUsername + " unblocked you.";
            case FOLLOW_BACK -> fromUsername + " followed you back.";
            case LIKE -> fromUsername + " liked your post.";
            case COMMENT -> fromUsername + " commented on your post.";
            case MENTIONED_IN_POST -> fromUsername + " mentioned you in a post.";
            case MENTIONED_IN_COMMENT -> fromUsername + " mentioned you in a comment.";
            default -> fromUsername + " sent a notification.";
        };
    }

    @Override
    public ApiResponse<List<NotificationResponse>> getNotificationsForUser() {
        User user = getLoggedInUser();
        log.info("Fetching notifications for user: {}", user.getUsername());

        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        List<NotificationResponse> response = notifications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        log.info("Fetched {} notifications for user '{}'", response.size(), user.getUsername());
        return ApiResponse.success(response, "Notifications fetched successfully");
    }

    @Override
    public ApiResponse<String> markAllAsRead() {
        User user = getLoggedInUser();
        log.info("Marking all notifications as read for user: {}", user.getUsername());

        notificationRepository.markAllAsRead(user.getId());
        log.info("All notifications marked as read for user '{}'", user.getUsername());

        return ApiResponse.success("All notifications marked as read.");
    }

    @Override
    public ApiResponse<Long> countUnreadNotifications() {
        User user = getLoggedInUser();
        log.info("Counting unread notifications for user: {}", user.getUsername());

        Long count = notificationRepository.countUnreadByReceiverId(user.getId());
        log.info("User '{}' has {} unread notifications", user.getUsername(), count);

        return ApiResponse.success(count, "Unread notification count.");
    }

    private NotificationResponse toDto(Notification notification) {
        log.debug("Mapping notification to DTO: {}", notification);
        return NotificationResponse.builder()
                .id(notification.getId())
                .senderUsername(notification.getSender().getUsername())
                .message(notification.getMessage())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .build();
    }

    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        log.debug("Authenticated username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username : " + username);
                });
    }
}
