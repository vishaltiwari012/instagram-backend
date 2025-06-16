package com.instagram.backend.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionResponseDto {
    private Long fromUserId;
    private Long toUserId;
    private String connectionType;
    private LocalDateTime createdAt;
}
