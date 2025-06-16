package com.instagram.backend.entity;

import com.instagram.backend.entity.enums.ConnectionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_connections",
        indexes = {
                @Index(name = "idx_from_user", columnList = "fromUser_id"),
                @Index(name = "idx_to_user", columnList = "toUser_id"),
                @Index(name = "idx_created_at", columnList = "createdAt")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User fromUser;

    @ManyToOne
    private User toUser;

    @Enumerated(EnumType.STRING)
    private ConnectionType connectionType;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}