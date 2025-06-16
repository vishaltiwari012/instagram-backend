package com.instagram.backend.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageRequestDTO {
    @NotNull(message = "Chat room ID cannot be null")
    private Long chatRoomId;

    @NotNull(message = "Sender ID cannot be null")
    private Long senderId;

    @NotNull(message = "Receiver ID cannot be null")
    private Long receiverId;

    @NotBlank(message = "Message content cannot be blank")
    private String content;
}
