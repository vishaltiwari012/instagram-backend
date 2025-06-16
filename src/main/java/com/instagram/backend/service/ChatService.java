package com.instagram.backend.service;

import com.instagram.backend.dtos.request.ChatMessageRequestDTO;
import com.instagram.backend.dtos.response.ChatMessageResponseDTO;
import com.instagram.backend.entity.enums.MessageStatus;

import java.util.List;

public interface ChatService {
    String findOrCreateChatRoom(Long userOneId, Long userTwoId);

    ChatMessageResponseDTO sendMessage(ChatMessageRequestDTO messageRequest);

    ChatMessageResponseDTO updateMessageStatus(Long messageId, MessageStatus status);

    List<ChatMessageResponseDTO> getMessagesByChatRoom(Long chatRoomId);
}
