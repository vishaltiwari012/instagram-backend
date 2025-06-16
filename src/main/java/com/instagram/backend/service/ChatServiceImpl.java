package com.instagram.backend.service;

import com.instagram.backend.dtos.request.ChatMessageRequestDTO;
import com.instagram.backend.dtos.response.ChatMessageResponseDTO;
import com.instagram.backend.entity.ChatMessage;
import com.instagram.backend.entity.ChatRoom;
import com.instagram.backend.entity.User;
import com.instagram.backend.entity.enums.MessageStatus;
import com.instagram.backend.exception.*;
import com.instagram.backend.repository.ChatMessageRepository;
import com.instagram.backend.repository.ChatRoomRepository;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final AuthenticationFacade authenticationFacade;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public String findOrCreateChatRoom(Long userOneId, Long userTwoId) {
        if (userOneId.equals(userTwoId)) {
            throw new IllegalArgumentException("Cannot create chat room with same user");
        }

        // Ensure consistent ordering
        Long firstId = Math.min(userOneId, userTwoId);
        Long secondId = Math.max(userOneId, userTwoId);

        User firstUser = userRepository.findById(firstId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + firstId));
        User secondUser = userRepository.findById(secondId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + secondId));

        ChatRoom chatRoom = chatRoomRepository.findByUsers(firstUser, secondUser)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .userOne(firstUser)
                            .userTwo(secondUser)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        return "Chat room created or found with ID: " + chatRoom.getId();
    }

    @Override
    @Transactional
    public ChatMessageResponseDTO sendMessage(ChatMessageRequestDTO messageRequest) {
         ChatRoom chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
                .orElseThrow(() -> new ChatRoomNotFoundException("Chat room not found with id: " + messageRequest.getChatRoomId()));

        User sender = userRepository.findById(messageRequest.getSenderId())
                .orElseThrow(() -> new UserNotFoundException("Sender not found with id: " + messageRequest.getSenderId()));

        User receiver = userRepository.findById(messageRequest.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException("Sender not found with id: " + messageRequest.getSenderId()));

        validateSenderInChatRoom(sender, chatRoom);
        validateMessageContent(messageRequest.getContent());

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .receiver(receiver)
                .content(messageRequest.getContent())
                .sentAt(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        return mapToResponseDTO(savedMessage);
    }

    @Override
    public ChatMessageResponseDTO updateMessageStatus(Long messageId, MessageStatus status) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        // 🛑 Avoid update if status is already same
        if (message.getStatus() == status) {
            return mapToResponseDTO(message); // No need to save or broadcast
        }

        // Set timestamps only if needed
        if (status == MessageStatus.DELIVERED && message.getDeliveredAt() == null) {
            message.setDeliveredAt(LocalDateTime.now());
        } else if (status == MessageStatus.SEEN && message.getSeenAt() == null) {
            message.setSeenAt(LocalDateTime.now());
        }

        message.setStatus(status);

        ChatMessage updatedMsg = chatMessageRepository.save(message);
        ChatMessageResponseDTO dto = mapToResponseDTO(updatedMsg);

        // ✅ Broadcast only if status changed
        messagingTemplate.convertAndSend(
                "/queue/chat/" + updatedMsg.getChatRoom().getId(),
                dto
        );

        return dto;
    }

    @Override
    public List<ChatMessageResponseDTO> getMessagesByChatRoom(Long chatRoomId) {
        // Validate that chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("Chat room not found with id: " + chatRoomId));

        User user = getLoggedInUser();
        if (!chatRoom.getUserOne().getId().equals(user.getId()) && !chatRoom.getUserTwo().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("User is not a participant in this chat room");
        }
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId);

        return messages.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }


//    Utility methods

    private User getLoggedInUser() {
        String username = authenticationFacade.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username : " + username);
                });
    }

    private void validateSenderInChatRoom(User sender, ChatRoom chatRoom) {
        if (!sender.equals(chatRoom.getUserOne()) && !sender.equals(chatRoom.getUserTwo())) {
            throw new InvalidSenderException("Sender (id=" + sender.getId() + ") is not part of chat room (id=" + chatRoom.getId() + ")");
        }
    }

    private void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidMessageException("Message content cannot be empty");
        }
    }

    private ChatMessageResponseDTO mapToResponseDTO(ChatMessage message) {
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .deliveredAt(message.getDeliveredAt())
                .seenAt(message.getSeenAt())
                .status(message.getStatus())
                .build();
    }
}

