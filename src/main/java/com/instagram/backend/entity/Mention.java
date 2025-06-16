package com.instagram.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "mentions",
        indexes = {
                @Index(name = "idx_mentioned_user", columnList = "mentionedUser_id"),
                @Index(name = "idx_post_id", columnList = "post_id"),
                @Index(name = "idx_comment_id", columnList = "comment_id"),
                @Index(name = "idx_created_at", columnList = "createdAt")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User mentionedUser;

    @ManyToOne(optional = false)
    private Post post;

    @ManyToOne
    private Comment comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
