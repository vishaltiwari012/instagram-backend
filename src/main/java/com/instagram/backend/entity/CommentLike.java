package com.instagram.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_likes",
        indexes = {
                @Index(name = "idx_comment_id", columnList = "comment_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_liked_at", columnList = "likedAt"),
                @Index(name = "idx_unique_comment_user_like", columnList = "comment_id, user_id", unique = true)
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private LocalDateTime likedAt = LocalDateTime.now();
}
