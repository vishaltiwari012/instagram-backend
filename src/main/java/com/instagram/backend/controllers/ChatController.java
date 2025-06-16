package com.instagram.backend.controllers;

import com.instagram.backend.dtos.response.ChatMessageResponseDTO;
import com.instagram.backend.entity.enums.MessageStatus;
import com.instagram.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat APIs")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "Create or retrieve chat room",
            description = "Creates a new chat room between two users if it doesn't exist, otherwise returns the existing chat room ID"
    )
    @PostMapping("/create-room")
    public ResponseEntity<String> createChatRoom(
            @RequestParam("userOneId") Long userOneId,
            @RequestParam("userTwoId") Long userTwoId) {

        log.info("Request to create or find chat room between user {} and user {}", userOneId, userTwoId);
        String response = chatService.findOrCreateChatRoom(userOneId, userTwoId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get messages by chat room",
            description = "Fetches all chat messages from the specified chat room"
    )
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessages(@PathVariable Long chatRoomId) {
        log.info("Fetching messages for chat room ID: {}", chatRoomId);
        return ResponseEntity.ok(chatService.getMessagesByChatRoom(chatRoomId));
    }

    @Operation(
            summary = "Update message status",
            description = "Updates the status (e.g., SEEN, DELIVERED) of a specific message"
    )
    @PutMapping("/messages/{messageId}/status")
    public ResponseEntity<ChatMessageResponseDTO> updateMessageStatus(
            @PathVariable Long messageId,
            @RequestParam MessageStatus status) {
        log.info("Updating message ID {} to status {}", messageId, status);
        ChatMessageResponseDTO updated = chatService.updateMessageStatus(messageId, status);
        return ResponseEntity.ok(updated);
    }
}
