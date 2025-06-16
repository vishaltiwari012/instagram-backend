package com.instagram.backend.controllers;

import com.instagram.backend.dtos.request.ChatMessageRequestDTO;
import com.instagram.backend.dtos.response.ChatMessageResponseDTO;
import com.instagram.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles incoming chat messages from clients over WebSocket.
     * Saves the message and broadcasts it to subscribers of the chat room topic.
     */
    @MessageMapping("/chat.sendMessage") // Full path: /app/chat.sendMessage
    public void handleChatMessage(@Payload ChatMessageRequestDTO messageRequest) {
        try {
            if (messageRequest == null || messageRequest.getContent() == null || messageRequest.getContent().trim().isEmpty()) {
                log.warn("Received empty or null chat message, ignoring.");
                return;
            }

            // Save message and get full DTO back
            ChatMessageResponseDTO savedMessage = chatService.sendMessage(messageRequest);

            // Broadcast to the chat topic specific to this chat room
            String destination = "/queue/chat/" + savedMessage.getChatRoomId();
            messagingTemplate.convertAndSend(destination, savedMessage);

            log.info("Sent message to chat room {}: {}", savedMessage.getChatRoomId(), savedMessage.getContent());
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
        }
    }
}
