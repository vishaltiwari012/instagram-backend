package com.instagram.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_sender_id", columnList = "sender_id"),
                @Index(name = "idx_created_at", columnList = "createdAt"),
                @Index(name = "idx_is_read", columnList = "isRead")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private boolean isRead = false;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private com.instagram.backend.entity.enums.NotificationType type;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;
}