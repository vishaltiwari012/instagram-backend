package com.instagram.backend.websocket;

import com.instagram.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketListener {

    private final OnlineUserService onlineUserService;
    private final JwtService tokenProvider;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket CONNECT - Session ID: {}", accessor.getSessionId());
        log.debug("Session attributes during connect: {}", accessor.getSessionAttributes());

        if (accessor.getSessionAttributes() == null) {
            log.warn("No session attributes found during WebSocket connect.");
            return;
        }

        String token = (String) accessor.getSessionAttributes().get("token");
        if (token == null || token.isBlank()) {
            log.warn("WebSocket connection attempt without token in session attributes.");
            return;
        }

        try {
            Long userId = tokenProvider.extractUserId(token);
            if (userId != null) {
                onlineUserService.userConnected(userId);
                log.info("✅ User [{}] connected via WebSocket.", userId);
            } else {
                log.warn("❌ Failed to extract userId from token during connect.");
            }
        } catch (Exception e) {
            log.error("Exception while extracting userId from token: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket DISCONNECT - Session ID: {}", accessor.getSessionId());

        if (accessor.getUser() != null) {
            try {
                Long userId = Long.parseLong(accessor.getUser().getName());
                onlineUserService.userDisconnected(userId);
                log.info("🔌 User [{}] disconnected from WebSocket.", userId);
            } catch (NumberFormatException e) {
                log.error("Invalid user ID format in WebSocket disconnect event: {}", accessor.getUser().getName(), e);
            }
        } else {
            log.warn("WebSocket disconnect event without authenticated user.");
        }
    }
}
